# homeappliance store

<img src="https://user-images.githubusercontent.com/47841725/126923291-542e3210-0b82-41a3-8e11-5f35ffeafc3b.jpg"  width="50%" height="50%">

# 온라인 가전제품 매장 (가전제품 구매 서비스)

# Table of contents

- [개인과제 - 가전제품 구매 서비스](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현)
    - [DDD 의 적용](#DDD-의-적용)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [비동기식 호출과 Eventual Consistency](#비동기식-호출과-Eventual-Consistency)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [API 게이트웨이](#API-게이트웨이)
  - [운영](#운영)
    - [Deploy/Pipeline](#deploypipeline)
    - [동기식 호출 / Circuit Breaker / 장애격리](#동기식-호출-circuit-breaker-장애격리)
    - [Autoscale (HPA)](#Autoscale-(HPA))
    - [Zero-downtime deploy (Readiness Probe)](#Zerodowntime-deploy-(Readiness-Probe))
    - [ConfigMap](#ConfigMap)
    - [Self-healing (Liveness Probe)](#self-healing-(liveness-probe))


# 서비스 시나리오

기능적 요구사항
1. 고객이 가전제품을 선택하여 주문(Order)한다
2. 고객이 결제(Pay)한다
3. 결제가 완료되면 주문 내역이 가전제품 매장(Ordermanagement)에 전달된다.
4. 가전제품 매장 담당자가 주문을 접수하고 해당 가전제품을 포장한다.
5. 가전제품 포장이 완료되면 해당 매장 소속의 배달기사가 배송(Delivery)을 시작한다.
6. 배송이 시작 되면 고객에게 배송 시작 정보를 메시지로 전달한다.
7. 고객이 주문을 취소할 수 있다.
8. 주문이 취소되면 배송 및 결제가 취소된다.
9. 배송이 취소되면 고객에게 배송 취소 정보를 메시지로 전달한다.
10. 고객이 주문상태를 중간중간 조회한다.
11. 주문/배송상태가 바뀔 때마다 고객이 마이페이지에서 상태를 확인할 수 있다.

비기능적 요구사항
1. 트랜잭션
  - 결제가 완료되어야만 주문이 완료된다 (결제가 되지 않은 주문건은 아예 거래가 성립되지 않아야 한다 Sync 호출)
2. 장애격리
  - 주문관리(Ordermanagement) 기능이 수행되지 않더라도 주문(Order)은 365일 24시간 받을 수 있어야 한다 Async (event-driven), Eventual Consistency 
  - 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다 Circuit breaker, fallback
3. 성능
  - 고객이 마이페이지에서 배송상태를 확인할 수 있어야 한다 CQRS

# 체크포인트

- 분석 설계
  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    
  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?
  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?
  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?

- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?

# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)
  <img src="https://user-images.githubusercontent.com/85722733/124564081-a9708780-de7b-11eb-93aa-42c819be9059.png"  width="80%" height="80%">

## Event Storming 결과
* MSAEZ를 활용한 모델링 결과: http://www.msaez.io/#/storming/5gBKmyuP3kNaddbx8DFE8K4spi13/c193a0f262ffad5062d1bae0fc587e60

### 이벤트 도출
<img src="https://user-images.githubusercontent.com/47841725/126949578-ffcc2013-1e0c-49a4-9c35-b2f8be4dd198.PNG"  width="80%" height="80%">

### 부적격 이벤트 탈락
<img src="https://user-images.githubusercontent.com/47841725/126949644-a774ff77-ad68-438e-a3a3-813f38be8481.PNG"  width="80%" height="80%">

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
      - '주문내역이 상점에 전달됨' 및 '주문상태 업데이트됨'은 이벤트에 의한 반응에 가까우므로 이벤트에서 제외
      - '마이페이지에서 조회됨'은 발생한 사실, 결과라고 보기 어려우므로 이벤트에서 제외
      - '배송상태를 문자메시지로 알림'은 다른팀에서 관심을 가질만한 사실이 아니고 message 서비스 내에서만 동작하므로 이벤트에서 제외

### 액터, 커맨드 부착하여 읽기 좋게
<img src="https://user-images.githubusercontent.com/47841725/126951013-ea71fec4-7f33-4530-bbfb-ffc531a6a5f4.PNG"  width="65%" height="65%">

### 어그리게잇으로 묶기
<img src="https://user-images.githubusercontent.com/47841725/126951215-8678bf2d-0303-425e-a005-ef1b0108b0e6.PNG"  width="80%" height="80%">

    - 고객의 주문, 상점의 주문관리, 결제의 결제이력, 배송의 배송이력은 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들끼리 묶어줌

### 바운디드 컨텍스트로 묶기

<img src="https://user-images.githubusercontent.com/47841725/126951368-bd2e0c4b-4733-4a07-8baf-0fedcaa971ff.PNG"  width="80%" height="80%">

    - 도메인 서열 분리 
        - Core Domain:  order, ordermanagement : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기는 order의 경우 1주일 1회 미만, ordermanagement의 경우 1개월 1회 미만
        - Supporting Domain:  delivery, message : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함
        - General Domain:   payment : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 (핑크색으로 이후 전환할 예정)

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

<img src="https://user-images.githubusercontent.com/47841725/126951488-3ba92cb8-3130-4ed9-8b54-7b92a32a078e.PNG"  width="80%" height="80%">

### 폴리시의 이동과 컨텍스트 매핑(점선은 Pub/Sub, 실선은 Req/Resp)을 통해 완성된 모형

<img src="https://user-images.githubusercontent.com/47841725/127060540-e480e2e3-54a0-4ea6-adb5-6788a571d547.PNG"  width="80%" height="80%">

### 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증

<img src="https://user-images.githubusercontent.com/47841725/127061125-e4f78d12-0d83-4669-8c58-cbd6005a39b1.PNG"  width="90%" height="90%">

    - 고객이 가전제품을 선택하여 주문한다 (ok)
    - 고객이 결제한다 (ok)
    - 결제가 완료되면 주문 내역이 가전제품 매장에 전달된다 (ok)
    - 가전제품 매장 담당자는 주문을 접수함과 동시에 가전제품을 포장한다 (ok)
    - 가전제품 포장이 완료되면 해당 매장 소속의 배달기사가 배송을 시작한다 (ok)
    - 배송이 시작되면 고객에게 배송 시작 정보를 메시지로 전달한다.

<img src="https://user-images.githubusercontent.com/47841725/127061183-1a13811a-7d41-4b59-a316-da047dad80f4.PNG"  width="90%" height="90%">
 
    - 고객이 주문을 취소할 수 있다 (ok)
    - 주문이 취소되면 배송 및 결제가 취소된다 (ok)
    - 배송이 취소되면 고객에게 배송 취소 정보를 메시지로 전달한다. (ok)
    - 고객이 주문상태를 중간중간 조회한다 (ok)
    - 주문/배송상태가 바뀔 때마다 고객이 마이페이지에서 상태를 확인할 수 있다 (ok)


### 비기능 요구사항에 대한 검증
<img src="https://user-images.githubusercontent.com/47841725/127063668-0e4912ba-515e-47f9-9fb5-4602aa2f5fed.PNG"  width="90%" height="90%">

    - 마이크로서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
        - 고객 주문시 결제처리:  결제가 완료되지 않은 주문은 절대 받지 않는다는 경영자의 오랜 신념(?)에 따라, ACID 트랜잭션 적용. 주문완료시 결제처리에 대해서는 Request-Response 방식 처리
	- 결제 완료시 점주연결 및 배송처리:  payment 에서 ordermanagement 마이크로서비스로 주문요청이 전달되는 과정에 있어서 ordermanagement 마이크로서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
        - 나머지 모든 inter-microservice 트랜잭션: 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.
	
        
## 헥사고날 아키텍처 다이어그램 도출
![image](https://user-images.githubusercontent.com/47841725/127098567-9973086c-5455-4a52-a79e-981f6089dfd0.png)

# 구현 

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 바운더리 컨텍스트 별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스 맟 뷰를 로컬에서 실행하는 방법은 아래와 같다.
|MSA|기능|port|URL|
| :--: | :--: | :--: | :--: |
|order| 주문 |8081|http://localhost:8081/orders|
|myPages| 마이페이지 |8081|http://localhost:8081/myPages|
|ordermanagement| 주문정보 관리 |8082|http://localhost:8082/ordermanagements|
|delivery| 배송 관리 |8083|http://localhost:8083/deliveries|
|payment| 결제 시스템 |8084|http://localhost:8084/payments|
|message| 배송정보 알림 |8085|http://localhost:8085/messages|
|gateway| 게이트웨이 |8088||
```
cd order
mvn spring-boot:run

cd ordermanagement
mvn spring-boot:run 

cd delivery
mvn spring-boot:run  

cd payment
mvn spring-boot:run 

cd message
mvn spring-boot:run 

cd gateway
mvn spring-boot:run 
```

## DDD 의 적용

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가? 

각 서비스 내에 도출된 핵심 Aggregate Root 객체를 Entity로 선언하였다. (주문(order), 결제(payment), 주문관리(ordermgmt), 배송(delivery), 메시지(Message))

주문관리 Entity (Ordermgmt.java)
```
@Entity
@Table(name="Ordermgmt_table")
public class Ordermgmt {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long orderMgmtId;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private Long itemId;
    private String itemName;
    private Integer qty;
    private Integer itemPrice;
    private String deliveryAddress;
    private String deliveryPhoneNumber;
    private String orderStatus;

    @PostPersist
    public void onPostPersist(){
    }

    @PostUpdate
    public void onPostUpdate(){
        if (this.orderStatus.equals("orderTaken")) {
            OrderTaken orderTaken = new OrderTaken();
            BeanUtils.copyProperties(this, orderTaken);
            orderTaken.publishAfterCommit();
        }
        else if (this.orderStatus.equals("orderCanceled")) {
            CancelOrderTaken cancelOrderTaken = new CancelOrderTaken();
            BeanUtils.copyProperties(this, cancelOrderTaken);
            cancelOrderTaken.publishAfterCommit();
        }
    }
    @PrePersist
    public void onPrePersist(){
    }

    public Long getOrderMgmtId() {
        return orderMgmtId;
    }

    public void setOrderMgmtId(Long orderMgmtId) {
        this.orderMgmtId = orderMgmtId;
    }
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public Integer getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(Integer itemPrice) {
        this.itemPrice = itemPrice;
    }
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    public String getDeliveryPhoneNumber() {
        return deliveryPhoneNumber;
    }

    public void setDeliveryPhoneNumber(String deliveryPhoneNumber) {
        this.deliveryPhoneNumber = deliveryPhoneNumber;
    }
    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

}
```

Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 하였고 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다 

OrdermgmtRepository.java
```
package homeappliancestore;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="ordermgmts", path="ordermgmts")
public interface OrdermgmtRepository extends PagingAndSortingRepository<Ordermgmt, Long>{
  Optional<Ordermgmt> findByOrderId(Long orderId);

}
```


- 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?

가능한 현업에서 사용하는 언어(유비쿼터스 랭귀지)를 모델링 및 구현 시 그대로 사용하려고 노력하였다.

- 적용 후 Rest API의 테스트

[주문 후 배송까지 진행되는 경우]
1. 주문하기 - POST (Command-order에 해당)
```
http POST localhost:8088/orders customerId=1 customerName="Kang" itemId=2 itemName="Air conditional" qty=3 itemPrice=1500 deliveryAddress="Gangnam" deliveryPhoneNumber="010-0123-4567" orderStatus="orderStarted"
```
![image](https://user-images.githubusercontent.com/47841725/127066617-fd871864-fce3-476f-8519-9f19061e8351.PNG)

2. 결제하기 - PUT (Command-pay에 해당)
```
http PATCH localhost:8088/payments/1 orderStatus="payApproved"
```
![image](https://user-images.githubusercontent.com/47841725/127067439-a5b18f64-afbd-4d4d-9882-5b862117dfdd.jpg)

3. 주문 승인하기 - PUT (Command-takeOrder에 해당)
```
http PATCH localhost:8088/ordermgmts/1 orderStatus="orderTaken"
```
![image](https://user-images.githubusercontent.com/47841725/127068260-52087836-550a-44d1-9931-b738aeb64d6b.PNG)

4. 배송 시작되는 메시지 전송
	- 메시지 전달 api 있다고 가정함
	- DB에는 저장하므로 GET으로 확인
```
# url끝에 해당 주문 정보인 orderId를 가진 messageId를 붙여준다.
# 이 예제에서는 orderId 1에 해당하는 messageId는 1이다.
http GET localhost:8088/messages/1
```
![image](https://user-images.githubusercontent.com/47841725/127069733-86d94f2f-1ea9-4960-8b12-702a43df7fea.PNG)

[주문 취소하는 경우]
1. 주문 취소하기 - PUT (Command-cancelOrder에 해당)
```
http PATCH localhost:8088/orders/1 orderStatus="orderCancel"
```
![image](https://user-images.githubusercontent.com/47841725/127069426-ceea0c3a-b47a-4100-99e7-2dfdadfe06ec.PNG)

2. 배송 취소되는 메시지 전송
```
# 주문 후 배송하는 경우 테스트 이후에 진행한 예제입니다.
# message는 배송 성공/실패 모두 DB에 저장하므로 messageId는 자동 증가되어 2가 되었음.
http GET localhost:8088/messages/2
```
![image](https://user-images.githubusercontent.com/47841725/127069739-b5c59ebe-b13a-4e57-92f9-1e06467127e3.PNG)

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 주문(order)->결제(payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 결제서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# Order.java (Entity)
    @PostPersist
    public void onPostPersist(){
        OrderPlaced orderPlaced = new OrderPlaced();
        BeanUtils.copyProperties(this, orderPlaced);
        orderPlaced.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        homeappliancestore.external.Payment payment = new homeappliancestore.external.Payment();
        // mappings goes here
        payment.setOrderId(orderPlaced.getOrderId());
        payment.setCustomerId(orderPlaced.getCustomerId());
        payment.setCustomerName(orderPlaced.getCustomerName());
        payment.setItemId(orderPlaced.getItemId());
        payment.setItemName(orderPlaced.getItemName());
        payment.setQty(orderPlaced.getQty());
        payment.setItemPrice(orderPlaced.getItemPrice());
        payment.setDeliveryAddress(orderPlaced.getDeliveryAddress());
        payment.setDeliveryPhoneNumber(orderPlaced.getDeliveryPhoneNumber());
        payment.setOrderStatus(orderPlaced.getOrderStatus());
        OrderApplication.applicationContext.getBean(homeappliancestore.external.PaymentService.class)
            .pay(payment);

    }
    # ...중략
```

- FallBack 처리 전
```
# (class) PaymentService.java - fallback 처리 전

package homeappliancestore.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="payment", url="http://localhost:8084")
public interface PaymentService {
    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void pay(@RequestBody Payment payment);

}
```
fall back이 없는 경우에는 결제시스템(Payment)가 다운되었을 때, 주문시스템(Order)으로 장애가 전파되어 주문을 받을 수 없습니다.

![image](https://user-images.githubusercontent.com/47841725/127079244-0df03f83-3c27-4bc9-a037-3305786470f8.png)


- FallBack 처리 후
```
# (class) PaymentService.java - fallback 처리 후

package homeappliancestore.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="payment", url="http://localhost:8084", fallback = PaymentServiceFallback.class)
public interface PaymentService {
    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void pay(@RequestBody Payment payment);

}
```
```
# (class) PaymentServiceFallback.java

package homeappliancestore.external;

import org.springframework.stereotype.Component;

@Component
public class PaymentServiceFallback implements PaymentService {

    @Override
    public void pay(Payment payment) {
        System.out.println("Circuit breaker has been opened. Fallback returned instead.");
    }

}

```

fall back 처리를 하면 결제시스템이 다운 되어도 주문시스템이 장애가 전파되지 않아 주문시스템은 계속 동작합니다. 즉 fallback 처리를 통해 장애를 격리 할 수 있습니다.
![image](https://user-images.githubusercontent.com/47841725/127079924-f7310e60-cc04-4c50-91a6-a810954375cb.png)


위코드를 실행하면 아래와 같은 문구가 나오는 것을 확인할 수 있습니다.
<img src="https://user-images.githubusercontent.com/47841725/127077061-77119b32-564e-4592-bdb9-3d60bbc492a7.PNG" width="80%" height="80%">


## 비동기식 호출과 Eventual Consistency 
(이벤트 드리븐 아키텍처)

- 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
주문 후 결제를 제외한 나머지 마이크로서비스 트랜잭션은 Pub/Sub 관계인 **SAGA**패턴으로 구현함.
Pub/Sub 이벤트를 보여주는 예 (주문 취소 하는 경우)

![image](https://user-images.githubusercontent.com/47841725/127081799-574f70fa-05fa-415e-8413-5f1d0bf1abd2.png)

- Correlation-key: 각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
findByOrderId를 통해 orderId값을 기준으로 건별로 처리하여 Correlation-key 관계를 형성합니다.
아래는 Correlation-key를 보여주는 한 가지 예입니다.

delivery에서 카프카 리스너를 통해 cancelOrderTaken(주문 취소) 이벤트를 받아서 배송 취소 cancelDelivery Policy를 호출 하는 과정입니다. getOrderId()를 호출하여 Correlation-key 연결을 하고 있습니다.

delivery 서비스의 PolicyHandler.java
```
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCancelOrderTaken_CancelDelivery(@Payload CancelOrderTaken cancelOrderTaken){

        if(!cancelOrderTaken.validate()) return;

        System.out.println("\n\n##### listener CancelDelivery : " + cancelOrderTaken.toJson() + "\n\n");

        // Logic //
        deliveryRepository.findByOrderId(cancelOrderTaken.getOrderId()).ifPresent(delivery->{
            delivery.setOrderStatus("cancelOrderTaken");
            deliveryRepository.save(delivery);
        });

    }
```

## 폴리글랏 퍼시스턴스
- Payment 서비스는 다른 곳에서 활용 될 수 있는 데이터라고 생각하여 DB를 분리하여 적용함. 인메모리 DB인 hsqldb 사용.

```xml
		<!-- <dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency> -->

        	<dependency>
			<groupId>org.hsqldb</groupId>
			<artifactId>hsqldb</artifactId>
            		<version>2.4.0</version>
			<scope>runtime</scope>
		</dependency>
```
- 변경 후에도 구동 및 서비스 정상 작동 확인 완료
```
# 주문을 먼저 한 후에
http POST localhost:8088/orders customerId=1 customerName="Kang" itemId=2 itemName="Air conditional" qty=3 itemPrice=1500 deliveryAddress="Gangnam" deliveryPhoneNumber="010-0123-4567" orderStatus="orderStarted"
```
- Payment 승인 PUT

![image](https://user-images.githubusercontent.com/47841725/127084025-8d0736c4-53b8-44c2-86d8-2e450333b36f.png)
- Payment 확인 GET

![image](https://user-images.githubusercontent.com/47841725/127084435-6a4a38e3-4e64-4f77-9053-6575abacae2a.png)

# 운영
## Deploy/Pipeline

ECR 생성
```
aws ecr create-repository --repository-name user02-eks-gateway --region ap-northeast-2
aws ecr create-repository --repository-name user02-eks-order --region ap-northeast-2
aws ecr create-repository --repository-name user02-eks-payment --region ap-northeast-2
aws ecr create-repository --repository-name user02-eks-ordermanagement --region ap-northeast-2
aws ecr create-repository --repository-name user02-eks-delivery --region ap-northeast-2
aws ecr create-repository --repository-name user02-eks-message --region ap-northeast-2
```

Build 및 ECR 에 Build/Push 하기
```
mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-gateway:v1.0 .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-gateway:v1.0

mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-order:v1.0 .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-order:v1.0

mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-payment:v1.0 .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-payment:v1.0

mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-ordermanagement:v1.0 .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-ordermanagement:v1.0

mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-delivery:v1.0 .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-delivery:v1.0

mvn package
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-message:v1.0 .
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user02-eks-message:v1.0
```

Deploy 및 Service 생성
```
cd ..
kubectl apply  -f order.yml
kubectl apply  -f payment.yml
kubectl apply  -f ordermanagement.yml
kubectl apply  -f delivery.yml
kubectl apply  -f message.yml
kubectl apply  -f gateway.yml
```

Kafka 실행 후 확인
![image](https://user-images.githubusercontent.com/47841725/127100743-4a0aca3b-72cd-4cef-bc19-1326fbdf31a0.png)

kubectl get all로 확인
![image](https://user-images.githubusercontent.com/47841725/127101158-2e6717fd-6f39-4d75-a083-9adc051fc2eb.png)



