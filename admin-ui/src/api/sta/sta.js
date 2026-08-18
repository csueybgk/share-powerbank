import request from '@/utils/request'

// 统计订单数据
export function getOrderCount(message) {
    return request({
      url: '/sta/orderData?message='+message,
      method: 'get'
    })
  }