<template xmlns:v-on="http://www.w3.org/1999/xhtml" xmlns:v-bind="http://www.w3.org/1999/xhtml" >
    <div >
        <el-col class="well well-lg" style="background-color: white;">
            <el-row>
                <el-col>
                    <el-form :model="filters" label-position="right" label-width="60px">
                        <el-col :span="5">
                            <el-form-item label="公司名:">
                                <el-input v-model="filters.customerName"
                                          placeholder="公司名"
                                          auto-complete="off"
                                          clearable></el-input>
                            </el-form-item>
                        </el-col>
                    </el-form>
                    <el-col :span="3" style="margin-left: 25px">
                        <el-button
		                        icon="el-icon-search"
		                        size="normal"
		                        type="primary"
		                        @click="search" >搜索
                        </el-button >
                    </el-col >

                    <el-button style="float: right;"
                               icon="el-icon-plus"
                               size="normal"
                               type="primary"
                               @click="handleAdd" >公司
                    </el-button >


                    <el-table
		                    v-loading="loadingUI"
		                    element-loading-text="获取数据中..."
		                    :data="tableData"
		                    border
		                    style="width: 100%;" >
                        <el-table-column
		                        width="75"
                                align="center"
		                        label="序号" >
                            <template scope="scope">
                                {{scope.$index+startRow}}
                            </template>
                        </el-table-column>
                        <el-table-column
                                align="center"
                                prop="customerName"
                                label="公司名称">
                        </el-table-column>
                        <el-table-column
                                align="center"
                                prop="createTime"
                                width="200"
                                label="创建时间" >
                        </el-table-column >
                        <el-table-column
                                align="center"
                                prop="updateTime"
                                width="200"
                                label="更新时间" >
                        </el-table-column >
                        <el-table-column
                                align="center"
                                label="操作"
                                width="200" >
                            <template scope="scope" >
                                <el-button
		                                size="small"
                                        icon="el-icon-edit"
                                        type="primary"
		                                @click="handleEdit(scope.$index, scope.row)" >编辑
                                </el-button >
                                <el-button
		                                size="small"
		                                type="danger"
                                        icon="el-icon-delete"
		                                :disabled="scope.row.account=='admin'"
		                                @click="handleDelete(scope.$index, scope.row)" >删除
                                </el-button >
                            </template >
                        </el-table-column >
                    </el-table >
                    <div class="block" style="text-align: center; margin-top: 20px" >
                        <el-pagination
                                background
		                        @current-change="handleCurrentChange"
		                        :current-page="currentPage"
		                        :page-size="pageSize"
		                        layout="total, prev, pager, next, jumper"
		                        :total="totalRecords" >
                        </el-pagination >
                    </div >
                </el-col >
            </el-row >
        </el-col >
        <el-dialog title="增加公司" :visible.sync="addDialogVisible" width="35%">
            <el-form :model="form" >
                <el-col :span="24">
                    <el-form-item label="公司名称：" :label-width="formLabelWidth">
                        <el-input v-model="form.customerName" @change="onChange"></el-input>
                    </el-form-item>
                </el-col >
            </el-form >
            <el-alert v-if="isError" style="margin-top: 10px;padding: 5px;"
                      :title="errorMsg"
                      type="error"
                      :closable="false"
                      show-icon >
            </el-alert >
            <div slot="footer" class="dialog-footer" style="margin-top: 30px" >
                <el-button @click="addDialogVisible = false" icon="el-icon-close" type="danger">取 消</el-button >
                <el-button type="primary" @click="onAdd" icon="el-icon-check">确 定</el-button >
            </div >
        </el-dialog >

        <el-dialog title="编辑公司" :visible.sync="modifyDialogVisible" width="35%">
            <el-form :model="modifyForm" >
                <el-col :span="24" >
                    <el-form-item label="公司名称：" :label-width="formLabelWidth">
                        <el-input v-model="modifyForm.customerName" @change="onChange" :disabled="modifyForm.account == 'admin'" ></el-input >
                    </el-form-item>
                </el-col>
            </el-form >
            <el-alert v-if="isError" style="margin-top: 10px;padding: 5px;"
                      :title="errorMsg"
                      type="error"
                      :closable="false"
                      show-icon >
            </el-alert >
            <div slot="footer" class="dialog-footer" style="margin-top: 30px" >
                <el-button @click="modifyDialogVisible = false" icon="el-icon-close" type="danger">取 消</el-button >
                <el-button type="primary" @click="onEidt" icon="el-icon-check">确 定</el-button >
            </div >
        </el-dialog >

        <el-dialog title="提示" :visible.sync="deleteConfirmVisible"  width="30%">
            <span >确认要删除[ <b >{{selectedItem.customerName}}</b > ]吗？</span >
            <span slot="footer" class="dialog-footer" >
	    <el-button @click="deleteConfirmVisible = false" icon="el-icon-close" >取 消</el-button >
	    <el-button type="primary" @click="onConfirmDelete" icon="el-icon-check">确 定</el-button >
	  </span >
        </el-dialog >
    </div >
</template >

<script >
    var _this;
    export default {
	    name: "part_manage",
	    components: {},
	    data () {
		    _this = this;
		    return {
			    isError: false,
			    errorMsg: '',
			    totalRecords: 0,
			    selectedItem: {},
			    deleteConfirmVisible: false,
			    tableData: [],
			    //分页
			    pageSize: EveryPageNum,//每一页的num
			    currentPage: 1,
			    startRow: 1,

			    //增加对话框
			    addDialogVisible: false,
			    form: {
			        createTime:"",
                    customerName: "",
			    },
			    formLabelWidth: '100px',

			    //增加对话框
			    modifyDialogVisible: false,
			    modifyForm: {
				    id: '',
                    customerName: "",
			    },
			    filters: {
                    customerName: ""
			    },
			    loadingUI: false,
		    }
	    },
	    methods: {
		    onChange: function () {
			    if (_this.addDialogVisible) {
				    _this.isError = _this.validateForm(_this.form, false);
			    }
			    else {
				    _this.isError = _this.validateForm(_this.modifyForm, true);
			    }
		    },


		    handleSizeChange(val) {
		    },
		    handleCurrentChange(val) {
			    this.currentPage = val;
			    this.onSelectUsers();
		    },
		    search() {
			    _this.onSelectUsers();
		    },

            onSelectUsers() {
			    _this.tableData = new Array();
			    _this.loadingUI = true;
                _this.filters.page = _this.currentPage;
                _this.filters.size = _this.pageSize;
			    $.ajax({
				    url: HOST + "/customer/search",
				    type: 'POST',
				    dataType: 'json',
				    data: _this.filters,
				    success: function (data) {
					    if (data.code == 200) {
						    _this.totalRecords = data.data.total;
                            _this.tableData = data.data.list;
                            _this.startRow = data.data.startRow;
					    }
                        _this.loadingUI = false;
				    },
                    error: function (data) {
                        showMessage(_this, '服务器访问出错', 0);
                        _this.loadingUI = false;
                    }
			    })
		    },


            handleAdd() {
                this.isError = false;
			    this.errorMsg = '';
			    this.addDialogVisible = true;
		    },

		    handleEdit(index, item) {
			    this.isError = false;
			    this.errorMsg = '';
			    this.selectedItem = item;
                this.modifyForm.id = item.id;
                this.modifyForm.customerName = item.customerName;
                this.isError = this.validateForm(this.modifyForm, true);
			    this.modifyDialogVisible = true;
		    },

		    handleDelete(index, item) {
			    this.selectedItem = copyObject(item);
			    if (this.selectedItem) {
				    _this.deleteConfirmVisible = true;
			    }
		    },

		    onConfirmDelete: function () {
			    _this.deleteConfirmVisible = false;
			    $.ajax({
				    url: HOST + "customer/delete",
				    type: 'POST',
				    dataType: 'json',
				    data: {"id":this.selectedItem.id},
				    success: function (data) {
					    if (data.code == 200) {
							_this.onSelectUsers();
						    showMessage(_this, '删除成功', 1);
					    } else {
						    showMessage(_this, '删除失败', 0);
					    }
				    },
				    error: function (data) {
					    showMessage(_this, '服务器访问出错', 0);
				    }
			    })
		    },

		    validateForm(formObj, isEdit)
		    {
			    var iserror = false;

			    if (!iserror && isStringEmpty(formObj.customerName)) {
				    iserror = true;
				    this.errorMsg = '公司名称不能为空！';
                }
			    return iserror;
		    },

		    onAdd() {
			    this.isError = _this.validateForm(this.form, false);

			    if (!this.isError) {
				    $.ajax({
					    url: HOST + "customer/add",
					    type: 'POST',
					    dataType: 'json',
					    data: {"customerName": _this.form.customerName},
					    success: function (data) {
						    if (data.code == 200) {
							    _this.onSelectUsers();
                                _this.addDialogVisible = false;
							    showMessage(_this, '添加成功', 1);
						    } else {
                                _this.isError = true;
                                _this.errorMsg = data.message;
						    }
					    },
					    error: function (data) {
						    _this.errorMsg = '服务器访问出错！';
					    }
				    })
			    }

		    },
		    onEidt() {
			    this.isError = this.validateForm(this.modifyForm, true);
			    if (!_this.isError) {
				    $.ajax({
					    url: HOST + "customer/update",
					    type: 'POST',
					    dataType: 'json',
					    data: {"customer":JSON.stringify(_this.modifyForm)},
					    success: function (data) {
						    if (data.code == 200){
							    _this.modifyDialogVisible = false;
							    _this.onSelectUsers();
							    showMessage(_this, '修改成功', 1);
						    }else {
                                _this.errorMsg = data.message;
                                _this.isError = true;
                            }
					    },
					    error: function (data) {
						    _this.errorMsg = '服务器访问出错！';
						    _this.isError = true;
					    }
				    })
			    }
		    }
	    },
	    computed: {},
	    filters: {

	    },
	    created: function () {
		    this.userinfo = JSON.parse(sessionStorage.getItem('user'));
		    if (isNull(this.userinfo)) {
			    this.$router.push({path: '/login'});
			    return;
		    }
	    },
	    mounted: function () {
		    this.onSelectUsers();
	    },
    }

</script >
<style >

</style >
