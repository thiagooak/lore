(ns lore.core
  (:require [clojure.data.json :as json]
            [io.pedestal.connector :as conn]
            [io.pedestal.http.http-kit :as hk]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.connector.test :as test])
  (:gen-class))

(def port (or (some-> (System/getenv "PORT")
                      parse-long)
              8080))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))


(def supported-types ["text/html"
                      "application/edn"
                      "application/json"
                      "text/plain"])

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content supported-types))

(defn accepted-type
  [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content
  [body content-type]
  (case content-type
    "text/html" body
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)))

(defn coerce-to
  [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

(defn missing-response-content-type?
  [context]
  (nil? (get-in context [:response :headers "Content-Type"])))

(def coerce-body-interceptor
  {:name ::coerce-body
   :leave
   (fn [context]
     (cond-> context
       (missing-response-content-type? context)
       (update :response coerce-to (accepted-type context))))})

(defn greet-handler [_request]
  {:status 200
   :body   "
            <form method='POST' action='/todo'>
            <input type='submit' />
            </form>
            <strong>Hello, world!<strong>\n"})

(def echo
  {:name  :echo
   :enter #(assoc % :response (ok (:request %)))})

(defonce *database (atom {}))

(def db-interceptor
  {:name :db-interceptor
   :enter
   (fn [context]
     (update context :request assoc :database @*database))
   :leave
   (fn [context]
     (if-let [tx-data (:tx-data context)]
       (let [database' (apply swap! *database tx-data)]
         (assoc-in context [:request :database] database'))
       context))})

(defn make-list [list-name]
  {:name  list-name
   :items {}})

(defn make-list-item [item-name]
  {:name  item-name
   :done? false})

(def list-create
  {:name :list-create
   :enter
   (fn [context]
     (let [list-name (get-in context [:request :query-params :name] "Unnamed List")
           new-list  (make-list list-name)
           db-id     (str (gensym "l"))
           url       (route/url-for :list-view :params {:list-id db-id})]
       (assoc context
              :response (created new-list "Location" url)
              :tx-data [assoc db-id new-list])))})

(defn find-list-by-id [dbval db-id]
  (get dbval db-id))

(def list-view
  {:name :list-view
   :enter
   (fn [context]
     (let [db-id    (get-in context [:request :path-params :list-id])
           the-list (when db-id
                      (find-list-by-id
                       (get-in context [:request :database])
                       db-id))]
       (cond-> context
         the-list (assoc :result the-list))))})

(def entity-render
  {:name :entity-render
   :leave
   (fn [context]
     (if-let [item (:result context)]
       (assoc context :response (ok item))
       context))})

(defn find-list-item-by-ids [dbval list-id item-id]
  (get-in dbval [list-id :items item-id] nil))

(def list-item-view
  {:name :list-item-view
   :leave
   (fn [context]
     (let [list-id (get-in context [:request :path-params :list-id])
           item-id (and list-id
                        (get-in context [:request :path-params :item-id]))
           item    (and item-id
                        (find-list-item-by-ids (get-in context [:request :database]) list-id item-id))]
       (cond-> context
         item (assoc :result item))))})

(defn list-item-add
  [dbval list-id item-id new-item]
  (if (contains? dbval list-id)
    (assoc-in dbval [list-id :items item-id] new-item)
    dbval))

(def list-item-create
  {:name :list-item-create
   :enter
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (let [item-name       (get-in context [:request :query-params :name] "Unnamed Item")
             new-item (make-list-item item-name)
             item-id  (str (gensym "i"))]
         (-> context
             (assoc :tx-data [list-item-add list-id item-id new-item])
             (assoc-in [:request :path-params :item-id] item-id)))
       context))})

(def routes
  #{["/" :get [coerce-body-interceptor
               content-negotiation-interceptor
               greet-handler] :route-name :greet]
    ["/echo" :get [coerce-body-interceptor
                   content-negotiation-interceptor
                   echo] :route-name :echo]
    ["/todo" :post [coerce-body-interceptor
                    content-negotiation-interceptor
                    db-interceptor
                    list-create]]
    ["/todo" :get [coerce-body-interceptor
                   content-negotiation-interceptor
                   echo] :route-name :list-query-form]
    ["/todo/:list-id" :get [coerce-body-interceptor
                            content-negotiation-interceptor
                            entity-render db-interceptor list-view]]
    ["/todo/:list-id" :post [entity-render list-item-view db-interceptor list-item-create]]
    ["/todo/:list-id/:item-id" :get [entity-render db-interceptor list-item-view]]
    ["/todo/:list-id/:item-id" :put echo :route-name :list-item-update]
    ["/todo/:list-id/:item-id" :delete echo :route-name :list-item-delete]})

(defn create-connector []
  (-> (conn/default-connector-map "0.0.0.0" port)
      (conn/with-default-interceptors)
      (conn/with-routes routes)
      (hk/create-connector nil)))

(defn -main [& _args]
  (conn/start! (create-connector)))

;; DEV

(defonce *connector (atom nil))

(defn start []
  (reset! *connector
          (conn/start! (create-connector))))

(defn stop []
  (conn/stop! @*connector)
  (reset! *connector nil))

(defn restart []
  (stop)
  (start))

(defn test-request [verb url]
  (test/response-for @lore.core/*connector verb url))

(comment
  (start)
  (restart)
  (stop)

  (test-request :get "/todo")
  (test-request :get "/todo/abcdef/12345")
  (test-request :get "/does-not-exist")

  (test/response-for @lore.core/*connector :get "/todo"))
