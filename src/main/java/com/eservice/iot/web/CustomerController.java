package com.eservice.iot.web;
import com.alibaba.fastjson.JSON;
import com.eservice.iot.core.Result;
import com.eservice.iot.core.ResultGenerator;
import com.eservice.iot.model.customer.Customer;
import com.eservice.iot.service.CustomerService;
import com.eservice.iot.service.impl.CustomerServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
* Class Description: xxx
* @author Wilson Hu
* @date 2018/08/21.
*/
@RestController
@RequestMapping("/customer")
public class CustomerController {
    @Resource
    private CustomerServiceImpl customerService;

    @PostMapping("/add")
    public Result add(@RequestParam String customerName) {
        if(customerName == null) {
            return ResultGenerator.genFailResult("公司名称为空！");
        } else {
            if(customerService.findBy("customerName", customerName) != null) {
                return ResultGenerator.genFailResult("公司名已存在！");
            } else {
                Customer customer = new Customer();
                customer.setCustomerName(customerName);
                customer.setCreateTime(new Date());
                customerService.save(customer);
                return ResultGenerator.genSuccessResult();
            }
        }
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        customerService.deleteById(id);
        return ResultGenerator.genSuccessResult();
    }

    @PostMapping("/update")
    public Result update(@RequestParam String customer) {
        Customer customerObj = (Customer) JSON.parseObject(customer, Customer.class);
        if(customerService.findBy("customerName",customerObj.getCustomerName()) != null) {
            return ResultGenerator.genFailResult("此公司名已存在！");
        } else {
            customerObj.setUpdateTime(new Date());
            customerService.update(customerObj);
            return ResultGenerator.genSuccessResult();
        }
    }

    @PostMapping("/detail")
    public Result detail(@RequestParam @NotNull Integer id) {
        Customer customer = customerService.findById(id);
        return ResultGenerator.genSuccessResult(customer);
    }

    @PostMapping("/search")
    public Result search(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "0") Integer size, String customerName) {
        PageHelper.startPage(page, size);
        List<Customer> list = customerService.search(customerName);
        PageInfo pageInfo = new PageInfo(list);
        return ResultGenerator.genSuccessResult(pageInfo);
    }
}
