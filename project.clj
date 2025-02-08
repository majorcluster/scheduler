(defproject scheduler "0.0.1-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :license {:name "MIT License"
                      :url "https://opensource.org/licenses/MIT"}
            :dependencies [[org.clojure/clojure "1.12.0"]
                           [io.pedestal/pedestal.service "0.7.2"]
                           [io.pedestal/pedestal.error "0.7.2"]
                           [io.pedestal/pedestal.jetty "0.7.2"]
                           [org.clojars.majorcluster/pedestal-api-helper "0.12.0"]
                           [org.clojars.majorcluster/clj-data-adapter "0.10.0"]
                           [org.clojure/data.json "2.5.1"]
                           [com.github.seancorfield/next.jdbc "1.3.981"]
                           [org.postgresql/postgresql "42.7.5"]
                           [com.outpace/config "0.13.5"]
                           [ch.qos.logback/logback-classic "1.5.16" :exclusions [org.slf4j/slf4j-api]]
                           [org.slf4j/jul-to-slf4j "2.0.16"]
                           [org.slf4j/jcl-over-slf4j "2.0.16"]
                           [org.slf4j/log4j-over-slf4j "2.0.16"]
                           [clj-jwt "0.1.1" :exclusions [org.bouncycastle/bcpkix-jdk15on]]
                           [clj-time "0.15.2"]
                           [http-kit/http-kit "2.8.0"]]
            :min-lein-version "2.0.0"
            :aliases {"config"               ["run" "-m" "outpace.config.generate"]
                      "migrate"              ["run" "-m" "scheduler.ports.sql.core/migrate"]
                      "diagnostics"          ["clojure-lsp" "diagnostics"]
                      "format"               ["clojure-lsp" "format" "--dry"]
                      "format-fix"           ["clojure-lsp" "format"]
                      "clean-ns"             ["clojure-lsp" "clean-ns" "--dry"]
                      "clean-ns-fix"         ["clojure-lsp" "clean-ns"]
                      "lint"                 ["do" ["diagnostics"]  ["format"] ["clean-ns"]]
                      "lint-fix"             ["do" ["format-fix"] ["clean-ns-fix"]]}
            :resource-paths ["config", "resources"]
            :jvm-opts ["-Duser.timezone=America/Sao_Paulo"]
            :profiles {:dev {:plugins [[com.github.clojure-lsp/lein-clojure-lsp "1.3.17"]]
                             :aliases {"run-dev"              ["trampoline" "run" "-m" "scheduler.server/run-dev"]
                                       "run-dev-w-migration"  ["trampoline" "run" "-m" "scheduler.server/run-dev-w-migration"]}
                             :dependencies [[io.pedestal/pedestal.service-tools "0.7.2"]]
                             :jvm-opts ["-Dresource.config.edn=dev-config.edn"]}
                       :test {:dependencies [[io.pedestal/pedestal.service-tools "0.7.2"]
                                             [com.h2database/h2 "2.3.232"]
                                             [nubank/matcher-combinators "3.9.1"]
                                             [nubank/mockfn "0.7.0"]]
                              :jvm-opts ["-Dresource.config.edn=test-config.edn"]}
                       :uberjar {:aot :all}}
            :main ^{:skip-aot true} scheduler.server)
