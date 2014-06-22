# Kerbal Space Program Mod Manager
This software is currently alpha! Be warned, there will be bugs. We'll love you if you try it anyway, and report any bugs you encounter in the [issue tracker](https://github.com/r04r/KSPModClient/issues). Please mention the git commit that your version is built off, if you do not know what this means or have used the auto updater just see the title of the application. It will look something like `f648d0f`.

You can always get the latest version by using the [auto updater](https://github.com/r04r/KSPModClient-launcher), downloadable from [here](http://ovh.minichan.org/ksp/KSPModManager-launcher.jar) (you only have to download this once). For a technical summary of recent changes, see [this](https://github.com/r04r/KSPModClient/commits/master).

If you wish to install from source, the following should get you set up (requires git, maven & jdk7):
````
git clone https://github.com/r04r/KSPModClient.git
cd KSPModClient
mvn clean compile assembly:single
java -jar target/KSPModManager*.jar
````
