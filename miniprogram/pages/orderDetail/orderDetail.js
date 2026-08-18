"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
new Proxy({}, {
  get(_, key) {
    throw new Error(`Module "console" has been externalized for browser compatibility. Cannot access "console.${key}" in client code.  See http://vitejs.dev/guide/troubleshooting.html#module-externalized-for-browser-compatibility for more details.`);
  }
});
const __default__ = {
  data() {
    return {};
  }
};
const _sfc_main = /* @__PURE__ */ Object.assign(__default__, {
  __name: "orderDetail",
  setup(__props) {
    const currentOrderId = common_vendor.ref("");
    const currentOrder = common_vendor.ref({});
    common_vendor.onLoad((option) => {
      currentOrderId.value = option.orderId;
    });
    const getOrderById = async () => {
      const result = await utils_request.request(`/order/orderInfo/getOrderInfo/${currentOrderId.value}`);
      if (result.code === 200) {
        currentOrder.value = result.data;
      }
    };
    common_vendor.onMounted(() => {
      getOrderById();
    });
    const toIndex = () => {
      common_vendor.index.reLaunch({
        url: "/pages/index/index"
      });
    };
    // 本地开发：模拟支付成功，跳过微信支付流程
    const pay = async () => {
      const result = await utils_request.request(`/order/orderInfo/simulatePaySuccess/${currentOrder.value.orderNo}`, {}, "POST");
      if (result.code === 200) {
        common_vendor.index.showToast({
          title: "支付成功",
          icon: "success"
        });
        setTimeout(() => {
          common_vendor.index.navigateTo({
            url: "/pages/order/order"
          });
        }, 1000);
      } else {
        common_vendor.index.showToast({
          title: result.msg || "支付失败",
          icon: "none"
        });
      }
    };
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: currentOrder.value.status === "0"
      }, currentOrder.value.status === "0" ? {} : {}, {
        b: currentOrder.value.status === "1"
      }, currentOrder.value.status === "1" ? {} : {}, {
        c: currentOrder.value.status === "2"
      }, currentOrder.value.status === "2" ? {} : {}, {
        d: common_vendor.t(currentOrder.value.duration === null ? 0 : currentOrder.value.duration),
        e: common_vendor.t(currentOrder.value.totalAmount),
        f: common_vendor.t(currentOrder.value.feeRule),
        g: common_vendor.t(currentOrder.value.createTime),
        h: common_vendor.t(currentOrder.value.startStationName),
        i: common_vendor.t(currentOrder.value.endTime === null ? "待归还" : currentOrder.value.endTime),
        j: common_vendor.t(currentOrder.value.endStationName === null ? "待归还" : currentOrder.value.endStationName),
        k: common_vendor.t(currentOrder.value.powerBankNo),
        l: common_vendor.t(currentOrder.value.orderNo),
        m: currentOrder.value.status === "1"
      }, currentOrder.value.status === "1" ? {
        n: common_vendor.o(pay)
      } : {
        o: common_vendor.o(toIndex)
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__file", "C:/Users/liuyuan/Desktop/test/guli-chongdian/pages/orderDetail/orderDetail.vue"]]);
wx.createPage(MiniProgramPage);
