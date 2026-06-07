<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100vh;">
    <el-card style="width: 400px;">
      <h2 style="text-align: center; margin-bottom: 24px;">Corum 로그인</h2>
      <el-form>
        <el-form-item>
          <el-input placeholder="아이디" />
        </el-form-item>
        <el-form-item>
          <el-input type="password" placeholder="비밀번호" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%">로그인</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
