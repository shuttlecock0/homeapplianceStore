
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import OrderManager from "./components/OrderManager"

import MyPage from "./components/MyPage"
import OrdermgmtManager from "./components/OrdermgmtManager"

import DeliveryManager from "./components/DeliveryManager"

import PaymentManager from "./components/PaymentManager"

import MessageManager from "./components/MessageManager"

export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/orders',
                name: 'OrderManager',
                component: OrderManager
            },

            {
                path: '/myPages',
                name: 'MyPage',
                component: MyPage
            },
            {
                path: '/ordermgmts',
                name: 'OrdermgmtManager',
                component: OrdermgmtManager
            },

            {
                path: '/deliveries',
                name: 'DeliveryManager',
                component: DeliveryManager
            },

            {
                path: '/payments',
                name: 'PaymentManager',
                component: PaymentManager
            },

            {
                path: '/messages',
                name: 'MessageManager',
                component: MessageManager
            },



    ]
})
