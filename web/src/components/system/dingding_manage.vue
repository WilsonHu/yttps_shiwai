<template xmlns:v-on="http://www.w3.org/1999/xhtml" xmlns:v-bind="http://www.w3.org/1999/xhtml" >
    <div >
        <el-col class="well well-lg" style="background-color: white;">
            <el-row>
                <el-col>
                    <el-form :model="filters" label-position="right" label-width="60px">
                        <el-col :span="5">
                            <el-form-item label="姓名:">
                                <el-input v-model="filters.name"
                                          placeholder="姓名"
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
                                prop="name"
                                label="姓名">
                        </el-table-column>
                        <el-table-column
                                align="center"
                                prop="userid"
                                label="UserId">
                        </el-table-column>
                    </el-table >
                </el-col >
            </el-row >
        </el-col >
    </div >
</template >

<script >
    var _this;
    export default {
	    name: "ding_manage",
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


			    formLabelWidth: '100px',

			    filters: {
                    name: ""
			    },
			    loadingUI: false,
		    }
	    },
	    methods: {

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
				    url: HOST + "/ding/list",
				    type: 'POST',
				    dataType: 'json',
				    data: _this.filters,
				    success: function (data) {
					    if (data.code == 200) {
                            _this.tableData = data.data;
					    }
                        _this.loadingUI = false;
				    },
                    error: function (data) {
                        showMessage(_this, '服务器访问出错', 0);
                        _this.loadingUI = false;
                    }
			    })
		    },
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
