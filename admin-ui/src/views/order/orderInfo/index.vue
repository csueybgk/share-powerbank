<template>
  <div class="app-container">

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="订单号" prop="orderNo">
        <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入订单号"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="充电宝编号" prop="powerBankNo">
        <el-input
            v-model="queryParams.powerBankNo"
            placeholder="请输入充电宝编号"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            @click="handleAdd"
            plain
            icon="Plus"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            :disabled="single"
            @click="handleUpdate"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="multiple"
            @click="handleDelete"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderInfoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="订单号" prop="orderNo" width="140"/>
      <el-table-column label="充电宝编号" prop="powerBankNo" width="120"/>
      <el-table-column label="借用站点" prop="startStationName" />
      <el-table-column label="归还站点" prop="endStationName" />
      <el-table-column label="状态" prop="status" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.status == '0'" type="warning">充电中</el-tag>
          <el-tag v-else-if="scope.row.status == '1'">待支付</el-tag>
          <el-tag v-else-if="scope.row.status == '2'" type="success">已支付</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" prop="totalAmount" width="100" />
      <el-table-column label="时长(分钟)" prop="duration" width="100" />
      <el-table-column label="借用时间" prop="startTime" width="160" />
      <el-table-column label="归还时间" prop="endTime" width="160" />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <el-dialog :title="title" v-model="open" width="650px" append-to-body>
      <el-form ref="orderInfoRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="form.orderNo" placeholder="请输入订单号" />
        </el-form-item>
        <el-form-item label="充电宝编号" prop="powerBankNo">
          <el-input v-model="form.powerBankNo" placeholder="请输入充电宝编号" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="借用站点" prop="startStationName">
              <el-input v-model="form.startStationName" placeholder="借用站点" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归还站点" prop="endStationName">
              <el-input v-model="form.endStationName" placeholder="归还站点" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option label="充电中" value="0" />
            <el-option label="待支付" value="1" />
            <el-option label="已支付" value="2" />
          </el-select>
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="借用时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归还时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="订单金额" prop="totalAmount">
              <el-input v-model="form.totalAmount" placeholder="金额" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时长(分钟)" prop="duration">
              <el-input v-model="form.duration" placeholder="时长" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="交易编号" prop="transactionId">
          <el-input v-model="form.transactionId" placeholder="请输入微信支付交易编号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="OrderInfo">
import { listOrderInfo, getOrderInfo, delOrderInfo, addOrderInfo, updateOrderInfo } from "@/api/order/orderInfo";

const { proxy } = getCurrentInstance();

const orderInfoList = ref([]);
const total = ref(0);
const loading = ref(true);
const showSearch = ref(true);
const title = ref("");
const open = ref(false);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: null,
    powerBankNo: null
  },
  form: {},
  rules: {
    orderNo: [
      { required: true, message: "订单号不能为空", trigger: "blur" }
    ],
    powerBankNo: [
      { required: true, message: "充电宝编号不能为空", trigger: "blur" }
    ]
  }
});
const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listOrderInfo(queryParams.value).then(response => {
    orderInfoList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function reset() {
  form.value = {
    id: null,
    orderNo: null,
    powerBankNo: null,
    startStationName: null,
    endStationName: null,
    status: null,
    startTime: null,
    endTime: null,
    totalAmount: null,
    duration: null,
    transactionId: null
  };
  proxy.resetForm("orderInfoRef");
}

function cancel() {
  open.value = false;
  reset();
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加订单";
}

function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getOrderInfo(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改订单";
  });
}

function submitForm() {
  proxy.$refs["orderInfoRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateOrderInfo(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addOrderInfo(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除选中的订单？').then(function() {
    return delOrderInfo(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getList()
</script>
