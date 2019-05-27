package com.wenba.ailearn.lib.live.model

import java.io.Serializable

/**
 * @author Jason
 * @description: 用户信息
 * @date :2019/4/28 2:41 PM
 */
data class UserModel(var userId: String, var password: String, var userType: Int) : Serializable