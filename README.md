This is a college project that uses Built OCSF. 
The project is about Parking Space, requesting to park, reserve a parking spot etc..
The project is not fully developed, in the working! 
In order to run client and server you might need jdk 24 (my jdk version that I worked with), javaFx (sdk 24), OCSF file and mysql!
In ProjectServer in MysqlConnection you should edit this line: 
Connection conn = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/parking?useSSL=false&serverTimezone=Asia/Jerusalem",
    "yourUsernameHere", "yourPasswordHere"); 
also notice that in this line my schema name was parking! --> "jdbc:mysql://localhost:3306/parking...
