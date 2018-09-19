<template>
    <div>
        <mt-header title="访客确认"></mt-header>
        <div v-if="!dataError" style="margin: 10px">
            <mt-field label="姓名:" :type="text" readonly v-model="visitorData.name" style="font-weight: bold"></mt-field>
            <mt-field label="手机:" :type="phone" readonly v-model="visitorData.phone" style="font-weight: bold"></mt-field>
            <el-row type="flex" justify="center" style="margin-top: 20px">
                <img  class="scale-img" :src="visitorImage()"/>
            </el-row>
            <el-row style="margin-top: 10px" type="flex" justify="center">
                <mt-button type="danger" @click="cancelVisitor()" style="min-width: 100px">拒  绝</mt-button>
                <mt-button type="primary" @click="acceptVisitor()" style="margin-left: 20px;min-width: 100px">同  意</mt-button>
            </el-row>
        </div>
        <div v-if="dataError">
            <p class="page-container">获取访客信息失败！</p>
        </div>
    </div>
</template>
<script>
    let _this;
    var defaultImgUrl = require('../assets/img/visitor_default.jpg')
    import { MessageBox } from 'mint-ui';
    import { Toast } from 'mint-ui';
    export default {
        name: "visitor",
        components: {},
        data() {
            _this = this;
            return {
                dataError: false,
                visitorId: "",
                visitorData: {
                    name: "",
                    phone: "",
                    imageId: ""
                }
            }
        },
        methods: {
            visitorImage() {
                if(this.visitorData.imageId == "") {
                    return defaultImgUrl;
                } else {
                    return this.visitorData.imageId;
                }
            },
            cancelVisitor() {
                MessageBox.confirm('拒绝访客?').then(action => {
                    $.ajax({
                        url: HOST + "/visitors/deleteVisitor",
                        type: 'POST',
                        dataType: 'json',
                        data:{
                            visitorId:_this.visitorId
                        },
                        success: function (data) {
                            if (data.code == 200) {
                                Toast({
                                    message: '操作成功',
                                });
                                window.location.reload();
                            }
                        },
                        error: function (data) {
                            Toast({
                                message: '操作失败',
                            });
                        }
                    })
                });
            },
            acceptVisitor() {
                MessageBox.confirm('同意访客?').then(action => {
                    $.ajax({
                        url: HOST + "/visitors/acceptVisitor",
                        type: 'POST',
                        dataType: 'json',
                        data:{
                            visitorId:_this.visitorId
                        },
                        success: function (data) {
                            if (data.code == 200) {
                                Toast({
                                    message: '操作成功',
                                });
                            }
                        },
                        error: function (data) {
                            Toast({
                                message: '操作失败',
                            });
                        }
                    })
                });

            }
        },
        created: function () {
            if (this.$route.query.visitor_id != null && this.$route.query.visitor_id != "") {
                this.visitorId = this.$route.query.visitor_id;
                $.ajax({
                    url: HOST + "/visitors/getVisitor",
                    type: 'POST',
                    dataType: 'json',
                    data:{
                        visitorId:_this.visitorId
                    },
                    success: function (data) {
                        if (data.code == 200) {
                            if(data.data.list.length > 0) {
                                _this.visitorData.name = data.data.list[0].person_information.name;
                                _this.visitorData.phone = data.data.list[0].person_information.phone;
                                _this.visitorData.imageId = data.data.list[0].face_list[0].face_image_id;
                            }
                        }
                    },
                    error: function (data) {
                        showMessage(_this, '服务器访问出错', 0);
                    }
                })
            } else {
                this.dataError = true;
            }
            this.dataError = false;
        },
    }

</script>
<style lang="scss" scoped>
    .scale-img{
        background-size:contain|cover;
        width:100%;
        height: 85%;
    }
</style>