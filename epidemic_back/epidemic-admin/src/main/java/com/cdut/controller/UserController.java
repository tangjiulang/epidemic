package com.cdut.controller;


import com.cdut.epidemic_common.utils.*;
import com.cdut.pojo.User;

import com.cdut.epidemic_common.utils.JWTUtil;
import com.cdut.epidemic_common.utils.MD5Util;
import com.cdut.epidemic_common.utils.RedisUtil;
import com.cdut.epidemic_common.utils.SaltUtils;

import com.cdut.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;

import static org.springframework.web.bind.annotation.RequestMethod.*;

//http://localhost:8088/swagger-ui.html
@Tag(name = "UserController", description = "用户管理")
@RestController
public class UserController {
    @Autowired
    UserServiceImpl userService;

    @Autowired
    private RedisUtil redisUtil;

    @Operation(description = "获取所有用户列表")
    @RequestMapping(value = "/getAllUsers", method = GET)
    public AjaxResult getAll() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return AjaxResult.success("所有用户获取成功",userService.getAll());
    }



    @Operation(description = "删除用户")
    @DeleteMapping(value = "/user/{id}")
    public AjaxResult delete(@PathVariable Integer id) {
        if(id==null||id<=0) return AjaxResult.error("用户ID非法");
        if (userService.deleteByID(id) == 0) {
            return AjaxResult.success("数据库中无该记录");
        }
        return AjaxResult.success("用户删除成功");
    }

    @Operation(description = "用户登录")
    @RequestMapping(value = "/user/login", method = POST)
    public AjaxResult login(HttpServletResponse response, @RequestParam("username") String username, @RequestParam("password") String password, @RequestParam("validateCode") String validateCode, @RequestHeader String validateKey) {
        String validateCodeRedis = (String) redisUtil.get(validateKey);
        System.out.println("1:"+validateCodeRedis);
        System.out.println("2:"+validateCode);
        if (validateCodeRedis.equals(validateCode)) {
            User user = userService.findByName(username);
            if (user != null) {
                String salt = user.getSalt();
                String finPassword = MD5Util.formPassToDBPass(password, salt);

                if (finPassword.equals(user.getPassword())) {
                    String token = JWTUtil.sign(user.getDisplayName(), user.getPassword());
                    response.setHeader("token", token);
                    return AjaxResult.success("登录成功",userService.getUserDto(user));
                } else {
                    return AjaxResult.error("密码错误");
                }
            } else {
                return AjaxResult.error("用户名不存在");
            }
        }else {
            return AjaxResult.error("验证码错误");
        }


    }

    @Operation(description = "用户注册")
    @RequestMapping(value = "/user/register",method = POST)
    public AjaxResult register(@Param("username")String username,@Param("password")String password){
        //禁止重名注册
        if (userService.getUserByName(username) != null) {
            return AjaxResult.error("用户名已存在");
        }

        User user = new User();
        user.setDisplayName(username);
        System.out.println("一次加密: " + password);
        System.out.println(username);
        user.setPassword(password);

        String salt = SaltUtils.getSalt(8);
        user.setSalt(salt);
        System.out.println(user.getSalt());

        user.setPassword( MD5Util.formPassToDBPass(password, salt));
        System.out.println(user.toString());


        return AjaxResult.success("注册成功", userService.register(user));
    }

    @Operation(description = "更新用户")
    @RequestMapping(value = "/register", method = PUT)
    public AjaxResult upDate(@RequestBody User user) {
//        //不允许用这个接口改密码
//        user.setPassword(null);
//        user.setSalt(null);

        if (user.getId() <= 0) {
            return AjaxResult.error("不存在该用户", -1);
        }

        int i = userService.update(user);
        return AjaxResult.success("更新成功", i);
    }

    @Operation(description = "ID查用户")
    @RequestMapping(value = "//user/{id}", method = GET)
    AjaxResult getUserById(@PathVariable("id") Integer id) {
        User userByID = userService.getUserByID(id);
        if (userByID == null) {
            return AjaxResult.error("该用户不存在");
        }
        return AjaxResult.success("获取成功", userByID);
    }
}
