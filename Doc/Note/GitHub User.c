*******关于GitHub团队协作的基本操作说明*******

//前序工作

一 注册GitHub帐号（包括绑定邮箱验证等等）略

二 建立工作文件夹
	2.1 通过Manager帐号"create a new repository"，设置为public或者private（月费7美刀就没有必要了），添加Project说明等等
	2.2 在开发电脑本地建立上传文件夹
	
三 下载 Git Bash 并安装（默认设置就好）略

四 进入 Git Bash 登录自己作为Developer的帐号
	4.1 Git配置
		git config --global user.name "用户名"
		git config --global user.email "邮箱"

五 公钥私钥配置
	5.1 生成公钥私钥
	ssh-keygen -t rsa -C "邮箱" 并按回车3下
	默认系统用户目录下.ssh文件夹里面生成私钥 id_rsa和公钥id_rsa.pub
	5.2 登录Manager和Developer的账户，共享上传公钥（私钥Manager保存就可以了）
	
//管理工作

