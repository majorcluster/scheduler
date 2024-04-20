(ns scheduler.ports.http-in.routes.core
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.body-params :as body-params]
   [scheduler.ports.http-in.routes.error-handler :refer [service-error-handler]]
   [scheduler.ports.http-in.routes.interceptors :as i]
   [scheduler.ports.http-in.routes.users :as r.users]))

(def json-public-interceptors [service-error-handler
                               (body-params/body-params)
                               (i/json-out)
                               http/html-body])

(def json-user-interceptors [service-error-handler
                             (body-params/body-params)
                             i/authz-user
                             (i/json-out)
                             http/html-body])

(def json-gateway-interceptors [service-error-handler
                                (body-params/body-params)
                                i/authz-user-gateway
                                (i/json-out)
                                http/html-body])

(def json-root-interceptors [service-error-handler
                             (body-params/body-params)
                             i/authz-admin
                             (i/json-out)
                             http/html-body])

(def specs #{["/users/signup" :post (conj json-public-interceptors `r.users/signup)]
             ["/users/verify-email/:token" :get (conj json-gateway-interceptors `r.users/verify-email)]
             ["/users/login" :post (conj json-public-interceptors `r.users/login)]
             ;["/users" :get (conj json-root-interceptors `r.events/get-events)]
             ;["/events/:id" :get (conj json-interceptors `r.events/get-event)]
             ;["/events" :post (conj json-root-interceptors `r.events/post-event)]
             ;["/events" :patch (conj json-root-interceptors `r.events/patch-event)]
             ;["/events/:id" :delete (conj json-root-interceptors `r.events/delete-event)]
             ;["/event-polls" :get (conj json-interceptors `r.event-polls/get-event-polls)]
             ;["/event-polls/:id" :get (conj json-interceptors `r.event-polls/get-event-poll)]
             ;["/event-polls" :post (conj json-root-interceptors `r.event-polls/post-event-poll)]
             })
