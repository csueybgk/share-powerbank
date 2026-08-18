import request from '@/utils/request'

// 查询柜机类型列表
export function listCabinetType(query) {
  return request({
    url: '/device/cabinetType/list',
    method: 'get',
    params: query
  })
}

export function addCabinetType(data) {
  return request({
    url: '/device/cabinetType',
    method: 'post',
    data: data
  })
}

// 查询柜机类型详细
export function getCabinetType(id) {
  return request({
    url: '/device/cabinetType/' + id,
    method: 'get'
  })
}

// 修改柜机类型
export function updateCabinetType(data) {
  return request({
    url: '/device/cabinetType',
    method: 'put',
    data: data
  })
}

// 获取全部柜机类型列表（不分页）
export function getCabinetTypeList() {
  return request({
    url: '/device/cabinetType/getCabinetTypeList',
    method: 'get'
  })
}

// 删除柜机类型
export function delCabinetType(id) {
  return request({
    url: '/device/cabinetType/' + id,
    method: 'delete'
  })
}