
# Web-PANDA

A Vaadin and Spring boot application to help with cheat detection in [Runestone](https://landing.runestone.academy/), a computer science education platform. Developed for Mr. Kwong at LHS.

The application pulls student data by generating a Runestone Session ID cookie and then using Runestone's API functions.

When analyzing a problem it will take into account large edits and small time differences between submissions of different problems. Submissions such as
- Submission 3 for Problem A having 500 characters changed from Submission 2 of the same problem, when most other submissions by students for this problem have changes of around 30 characters.
- Submission 4 of Problem B at 4:15 PM having 200 characters changed from the previous submission, where the student submitted code for Submission 3 of Problem A one minute earlier, at 4:14 PM. This probably means that they changed 200 characters in possibly less than a minute, which is usually cause for suspicion.

would be flagged for being suspicious.
## Run Locally

Clone the project

```bash
  git clone https://link-to-project
```

Go to the project directory

```bash
  cd my-project
```

Run

`./mvnw` on Mac/Linux, `mvnw` on Windows

IntelliJ should automatically generate a run configuration as well.

**The frontend is hosted on port 8080.**
## Build for Production

Run `mvn clean package -Pproduction` to package the jar into the folder `targets/name-version.jar`. `-Pproduction` tells Maven to build the JAR in the production profile, and so it will keep the JAR from being bloated. Make sure to delete the target folder once you're done using the JAR, or running the server normally will be in production mode as well.
## Project structure

- `MainLayout.java` in `src/main/java/com/github/williamli0707/views` contains the navigation setup (i.e., the
  side/top bar and the main menu). This setup uses
  [App Layout](https://vaadin.com/docs/components/app-layout).
- `views` package in `src/main/java/com/github/williamli0707` contains the server-side Java views of the application.
- `themes` folder in `frontend/` contains the custom CSS styles.
## Acknowledgements

- [Base Vaadin and intiial RunestoneAPI request methods](//https://github.com/caupcakes/runestone-submission-downloader)


## License

[MIT](https://choosealicense.com/licenses/mit/)

