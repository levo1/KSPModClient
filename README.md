Auto updater can be found at: http://ovh.minichan.org/ksp/KSPModManager-launcher.jar

To install from Git:
````
git clone https://github.com/r04r/KSPModClient.git
cd KSPModClient
mvn clean compile assembly:single
java -jar target/KSPModManager*.jar
````