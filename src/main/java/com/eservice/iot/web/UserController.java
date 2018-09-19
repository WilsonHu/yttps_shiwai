package com.eservice.iot.web;
import com.eservice.iot.core.Result;
import com.eservice.iot.core.ResultGenerator;
import com.eservice.iot.model.user.User;
import com.eservice.iot.service.impl.UserServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
* Class Description: xxx
* @author Wilson Hu
* @date 2018/08/21.
*/
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserServiceImpl userService;

    /**
     * 该值为default值， Android端传入的参数不能为“0”
     */
    private static String ZERO_STRING = "0";

    @PostMapping("/add")
    public Result add(@RequestBody @NotNull User user) {
        if(user.getPassword() == null) {
            return ResultGenerator.genFailResult("密码不存在!");
        } else if (user.getAccount() == null || user.getAccount().isEmpty()) {
            return ResultGenerator.genFailResult("用户名不存在或为空！");
        } else if(userService.findBy("account", user.getAccount()) != null){
            return ResultGenerator.genFailResult("账号已存在！");
        }else {
            userService.save(user);
            return ResultGenerator.genSuccessResult();
        }
    }

    @PostMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        userService.deleteById(id);
        return ResultGenerator.genSuccessResult();
    }

    @PostMapping("/update")
    public Result update(@RequestBody @NotNull User user) {
        userService.update(user);
        return ResultGenerator.genSuccessResult();
    }

    @PostMapping("/detail")
    public Result detail(@RequestParam @NotNull Integer id) {
        User user = userService.findById(id);
        return ResultGenerator.genSuccessResult(user);
    }

    @PostMapping("/list")
    public Result list(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "0") Integer size) {
        PageHelper.startPage(page, size);
        List<User> list = userService.findAll();
        PageInfo pageInfo = new PageInfo(list);
        return ResultGenerator.genSuccessResult(pageInfo);
    }

    @PostMapping("/requestLogin")
    public Result requestLogin(@RequestParam String account, @RequestParam String password) {

        if(account == null || "".equals(account)) {
            return ResultGenerator.genFailResult("账号不能为空！");
        } else if(password == null || "".equals(password)) {
            return ResultGenerator.genFailResult("密码不能为空！");
        }else {

            User user = userService.requestLogin(account, password);
            if(user == null) {
                return ResultGenerator.genFailResult("账号或密码不正确！");
            }else {
                user.setPassword("");
                return ResultGenerator.genSuccessResult(user);
            }
        }
    }
}
