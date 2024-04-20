# scheduler

FIXME

## Getting Started

:exclamation: Change usernames and passwords for PROD or internet-facing environments!

1. Start postgresql `docker run -itd -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -p 5432:5432 -v ~/work/postgres:/var/lib/postgresql/data --name postgresql postgres`
2. `$ docker exec -it postgresql bash`
3. <pre>PGPASSWORD=admin psql -U admin</pre>"
4. <pre>CREATE DATABASE scheduler;</pre>
5. <pre>\c scheduler;</pre>
6. <pre>CREATE EXTENSION IF NOT EXISTS "uuid-ossp";</pre>
7. Start the application: `EMAIL_FROM=my-email@any.com EMAIL_API_KEY=<my api key> EMAIL_COMPANY=<my company> EMAIL_URL=<my email api> lein with-profile dev run`
8. Start the application with migration: `EMAIL_FROM=my-email@any.com EMAIL_API_KEY=<my api key> EMAIL_COMPANY=<my company> EMAIL_URL=<my email api> lein with-profile dev run --migrate`
9. Start dev server: `EMAIL_FROM=my-email@any.com EMAIL_API_KEY=<my api key> EMAIL_COMPANY=<my company> EMAIL_URL=<my email api> lein run-dev-w-migration` or `EMAIL_FROM=my-email@any.com EMAIL_API_KEY=<my api key> EMAIL_COMPANY=<my company> EMAIL_URL=<my email api> lein run-dev`
10. Go to [localhost:8080](http://localhost:8080/scheduler) to see a nice list in json!
11. Read your app's source code at src/scheduler/service.clj. Explore the docs of functions
    that define routes and responses.
12. Run your app's tests with `lein test`.

## Migrations
They are located at `resources/migrations`.   
Must be named in number sequenced order.   
Run with: `$ lein run migrate`

## Tests
`lein test`

## Configuration

To configure logging see config/logback.xml. By default, the app logs to stdout and logs/.
To learn more about configuring Logback, read its [documentation](http://logback.qos.ch/documentation.html).


## Developing your service

1. Start a new REPL: `lein repl`
2. Start your service in dev-mode: `(def dev-serv (run-dev))`
3. Connect your editor to the running REPL session.
   Re-evaluated code will be seen immediately in the service.

### [Docker](https://www.docker.com/) container support

1. Configure your service to accept incoming connections (edit service.clj and add  ::http/host "0.0.0.0" )
2. Build an uberjar of your service: `lein uberjar`
3. Build a Docker image: `sudo docker build -t scheduler .`
4. Run your Docker image: `docker run -p 8080:8080 scheduler`
