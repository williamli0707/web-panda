
# Web-PANDA

A Vaadin and Spring boot application to help with cheat detection in [Runestone](https://landing.runestone.academy/), a computer science education platform. Developed for Mr. Kwong at LHS.

The application pulls student data by generating a Runestone Session ID cookie and then using Runestone's API functions.

When analyzing a problem it will take into account large edits and small time differences between submissions of different problems. Submissions such as
- Submission 3 for Problem A having 500 characters changed from Submission 2 of the same problem, when most other submissions by students for this problem have changes of around 30 characters.
- Submission 4 of Problem B at 4:15 PM having 200 characters changed from the previous submission, where the student submitted code for Submission 3 of Problem A one minute earlier, at 4:14 PM. This probably means that they changed 200 characters in possibly less than a minute, which is usually cause for suspicion.

would be flagged for being suspicious.

The application will prompt a Runestone username and password. This is used to access Runestone data and can be changed later. The default website password is "password". 

The website passcode can be found if forgotten. Go to the folder where the Java Preferences API stores its keys: 
- `~/Library/Preferences/com.github.williamli0707.plist` on MacOS
- `~/.java/.userPrefs/com/github/williamli0707/webpanda/prefs.xml` on Linux
- ??? on Windows (probably in the appData folder)

## Run Locally

Clone the project

```bash
  git clone git@github.com:williamli0707/web-panda.git
```

Go to the project directory

```bash
  cd web-panda
```

**Change the MongoDB URI in src/main/resources/application.properties. Change the cluster name as needed.**

Run

`./mvnw` on Mac/Linux, `mvnw` on Windows

IntelliJ should automatically generate a run configuration as well.

**The frontend is hosted on port 8080.**
## Build for Production

**Change the MongoDB URI in src/main/resources/application.properties. Change the cluster name as needed.**

Run `mvn clean package -Pproduction` to package the jar into the folder `targets/name-version.jar`. `-Pproduction` tells Maven to build the JAR in the production profile, and so it will keep the JAR from being bloated. Make sure to delete the target folder once you're done using the JAR, or running the server normally will be in production mode as well.
## Project structure

- `MainLayout.java` in `src/main/java/com/github/williamli0707/views` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/docs/components/app-layout).
- `views` package in `src/main/java/com/github/williamli0707` contains the server-side Java views of the application.
- `themes` folder in `frontend/` contains the custom CSS styles.
## Known Issues

Running the application, the log will occasionally show an error similar to
```
java.lang.IllegalArgumentException: Invalid character found in method name [0x160x030x010x00....]. HTTP method names must be tokens
 at org.apache.coyote.http11.Http11InputBuffer.parseRequestLine(Http11InputBuffer.java:407) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:264) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:896) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1744) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-10.1.15.jar!/:na]
        at java.base/java.lang.Thread.run(Thread.java:833) ~[na:na]
```

[This issue has been documented](https://stackoverflow.com/questions/42218237/tomcat-java-lang-illegalargumentexception-invalid-character-found-in-method-na) and it is due to clients sending HTTPS requests when the server expects HTTP requests. It is fixable by using HTTP to connect to the server instead of HTTPS. 
## Acknowledgements

- [Base Vaadin and intiial RunestoneAPI request methods](https://github.com/caupcakes/runestone-submission-downloader)


## License

[MIT](https://choosealicense.com/licenses/mit/)

