buino
=====

Small android app + related server that receives build status notifications from Jenkins


Server
------
The small java jetty server needs to be running (see ServerStartup.java) and your Jenkins needs the [Notification Plugin]https://wiki.jenkins-ci.org/display/JENKINS/Notification+Plugin sending out notifications to the very same server. 
This java server is basically used to store & synchronize notifications with the [Parse]https://parse.com/ service, which can then push them to mobile devices (among other things).


App
---
Once your java server is running, the app will automatically receive push notifications via the afforementioned [Parse]https://parse.com/ service. Creating a new user is optional. Some screenshots:

![Screenshot1](http://wp.me/a1KcBm-o)

![Screenshot2](http://beerdeveloper.files.wordpress.com/2014/04/buino-ss2.png)

![Screenshot3](http://beerdeveloper.files.wordpress.com/2014/04/buino-ss3.png)

