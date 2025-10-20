git config --global url."https://hub.fastgit.xyz/".insteadOf "https://github.com/"
git config --global url."https://hub.fastgit.xyz/".insteadOf "git://github.com/"
# 取消 FastGit 代理:
# git config --global --unset url."https://hub.fastgit.xyz/".insteadOf



切目录

C:\> D:
D:\>  

C:\Users> cd /d D:\your_folder_on_D_drive
D:\your_folder_on_D_drive>



未验证：

linux 删除除了当前分支外的其他分支

git branch | grep -v "$(git rev-parse --abbrev-ref HEAD)" | xargs git branch -D

`-v` 选项表示 "反向匹配"，即只输出**不包含**指定模式的行。

`"$(git rev-parse --abbrev-ref HEAD)"` 则是动态获取到的当前分支名。





git remote -v

类似这样的输出：

```
origin  https://github.com/your-username/your-fork.git (fetch)
origin  https://github.com/your-username/your-fork.git (push)
upstream    https://github.com/original-owner/original-repo.git (fetch)
upstream    https://github.com/original-owner/original-repo.git (pu
```

删除fork源

git remote remove upstream 



默认使用自己的

git merge -s ours <要合并的远程分支或本地分支名称>







android studio git 功能消失

- 点击菜单栏：**File (文件) -> Settings (设置)** (macOS 上是 **Android Studio -> Preferences (偏好设置)**)。
- 在左侧导航栏中，找到 **Version Control (版本控制)**。
- 在右侧面板中，会看到一个列表。确保的项目根目录被列出，并且对应的列显示为Git。
  - **如果未列出：** 点击右侧的 `+` 号，选择的项目根目录，然后点击 **OK**。
  - **如果已列出但不是 Git：** 选中它，点击 `-` 号移除，然后重新点击 `+` 号添加，确保选择 Git。
  - **如果已列出且是 Git：** 尝试选中它，点击 `-` 号移除，然后重新点击 `+` 号添加。
- 点击 **Apply (应用)** 和 **OK** 保存设置。



##### git 停止跟踪一个已经跟踪的文件，不影响已有文件

1. **停止 Git 跟踪文件：** 

   Bash

   ```
   git rm --cached src/test/output/merged_output.txt
   ```

   这个命令会从 Git 的索引中移除 `merged_output.txt`，但会保留本地的副本。

2. **提交更改：** 现在，需要提交这个更改，告诉 Git 不再希望跟踪这个文件：

   Bash

   ```
   git commit -m "Stop tracking src/test/output/merged_output.txt"
   ```



假设删除了名为 `feature-x` 的分支，并且通过 `git reflog` 找到了删除前最后一次提交的commit ID 是 `a1b2c3d`。

那么，恢复步骤如下：



1. `git reflog` （查找删除前最后一次提交）
2. `git checkout -b feature-x-recovered a1b2c3d` （创建新的分支）
3. `git log feature-x-recovered` 或 `git show a1b2c3d` (验证)
