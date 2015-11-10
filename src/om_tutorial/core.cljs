(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [datascript.core :as d]
            [cljs.pprint :as pp :refer [pprint]]))

(enable-console-print!)

;________________________________________________________________________
;                                                                        |
; An attempt to display and mutate recursive data hold into DataScript   |
; Works with [org.omcljs/om "1.0.0-alpha19-SNAPSHOT"] as of 2015-11-09   |
;________________________________________________________________________|



;__________________________________________________________
;                                                          |
;                DB                                        |
;__________________________________________________________|

(def conn (d/create-conn {:task/children {:db/valueType :db.type/ref
                                          :db/isComponent true
                                          :db/cardinality :db.cardinality/many}
                          :task/creator {:db/valueType :db.type/ref}}))



(d/transact! conn [{:db/id         -1
                    :task/title    "Om-Next Beta version"
                    :task/status :wip
                    :task/children [{:db/id         -2
                                     :task/title    "Handle recursive query"
                                     :task/status :open
                                     :task/children [{:db/id -3
                                                      :task/title "Think a little harder"
                                                      :task/status :open}]}
                                    {:db/id -4
                                     :task/title "Hammock a little more"
                                     :task/status :open}]}])


;__________________________________________________________
;                                                          |
;                UI                                        |
;__________________________________________________________|


(declare task-ui)

(defui TaskUI
       static om/IQuery
       (query [this]
              '[:db/id :task/title :task/status {:task/children ...}]);Magic appends with the recursice notation
       Object
       (render [this]
               (let [{:keys [db/id task/title task/status task/children] :as task} (om/props this)]
                 (dom/div #js {:style #js {:border "1px solid grey"
                                           :borderRadius "4px"
                                           :margin "3px"
                                           :padding "3px"}}
                          (dom/label #js {:style #js {:display "flex"}}
                                  (dom/button #js {:onClick #(om/transact! this `[(task/close ~task)])} "x")
                                  (str (name status) " - " title))
                          (apply dom/div nil (map task-ui children))))))


(def task-ui (om/factory TaskUI {:keyfn :db/id}))

(defui TasksList
       static om/IQuery
       (query [this]
              `[{:tasks/all-tasks ~(om/get-query TaskUI)}])
       Object
       (render [this]
               (let [{:keys [tasks/all-tasks]} (om/props this)]
                 (apply dom/div #js {}
                        (for [task all-tasks]
                          (task-ui task))))))

;__________________________________________________________
;                                                          |
;                Read                                      |
;__________________________________________________________|


(defmulti read om/dispatch)

(defmethod read :tasks/all-tasks
  [{:keys [state selector]} key params]
  {:value (d/q '[:find [(pull ?e ?selector) ...]
                 :in $ ?selector
                 :where
                 [?e :task/title]]
               (d/db state) selector)})


;__________________________________________________________
;                                                          |
;                Mutation                                  |
;__________________________________________________________|

(defmulti mutate om/dispatch)

(defmethod mutate 'task/close
  [{:keys [state]} _ {:keys [db/id] :as params}]
  {:action #(d/transact! conn [{:db/id id :task/status :closed}])})



;__________________________________________________________
;                                                          |
;                Reconciler                                |
;__________________________________________________________|

(def parser (om/parser {:read read
                        :mutate mutate}))



(def reconciler (om/reconciler {:parser parser
                                :state conn}))


;__________________________________________________________
;                                                          |
;                Root                                      |
;__________________________________________________________|

(om/add-root! reconciler TasksList (gdom/getElement "app"))


(comment
  (pprint (-> (get-in reconciler [:config :indexer]))))

