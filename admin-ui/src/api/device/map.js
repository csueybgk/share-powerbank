import request from '@/utils/request'

export function calculateLatLng(keyword) {
  return request({
    url: '/device/map/calculateLatLng/'+keyword,
    method: 'get'
  })
}