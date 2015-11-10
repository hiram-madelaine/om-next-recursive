(ns om-tutorial.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [datascript.core :as d]
            [cljs.pprint :as pp :refer [pprint]]))

(enable-console-print!)

;________________________________________________________________________
;                                                                        |
;  An attempt to display and mutate recursive data hold into DataScript  |
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
                    :task/creator -9
                    :task/status :wip
                    :task/children [{:db/id         -2
                                     :task/creator -7
                                     :task/title    "Handle recursive query"
                                     :task/status :open
                                     :task/children [{:db/id -3
                                                      :task/creator -7
                                                      :task/title "Think a little more"
                                                      :task/status :open}]}
                                    {:db/id -4
                                     :task/creator -8
                                     :task/title "Hammock a little more"
                                     :task/status :open}]}
                   {:db/id -5
                    :tag/title "milestone 1"
                    :tag/important? true}
                   {:db/id -6
                    :tag/title "milestone 2"
                    :tag/important? false}
                   {:db/id -7
                    :user/name "thomasdeutsh"}
                   {:db/id -8
                    :user/name "hmadelaine"}
                   {:db/id -9
                    :user/name "dnolen"}])

;__________________________________________________________
;                                                          |
;                UI                                        |
;__________________________________________________________|


(declare task-ui)  ; Needed because of the recursion

(defui TaskView
       static om/IQuery
       (query [this]
              '[:db/id :task/title {:task/creator [:db/id :user/name]} :task/status {:task/children ...}]) ;Magic appends with the recursice notation
       Object
       (render [this]
               (let [{:keys [db/id task/title task/status task/children task/creator] :as task} (om/props this)]
                 (dom/div (clj->js {:style {:border       "1px solid grey"
                                            :borderRadius "4px"
                                            :margin       "3px"
                                            :padding      "3px"
                                            :maxWidth    "400px"}})
                          (dom/div (clj->js {:style {:display        "flex"
                                                     :justifyContent "space-between"}})
                                   (str "@" (:user/name creator))
                                   title
                                   (name status)
                                   (dom/button #js {:onClick #(om/transact! this `[(task/close ~task)])} "Close"))
                          (apply dom/div nil (map task-ui children))))))


(def task-ui (om/factory TaskView {:keyfn :db/id}))


(defui TagView
       static om/IQuery
       (query [this]
              '[:db/id :tag/title :tag/important?])
       Object
       (render [this]
               (let [{:keys [tag/title tag/important?]} (om/props this)]
                 (dom/div #js{} title))))

(def tag-view (om/factory TagView {:keyfn :db/id}))




#_(defui taskEdit
       Object
       (render [this]
               (let [{:keys [task tags]}  (om/get-computed this)])))


(defui TagsSelect
       Object
       (render [this]
               (let [{:keys [tags/all-tags]} (om/props this)]
                 (dom/select #js {}
                             (for [{:keys [tag/title db/id] :as tag} all-tags]
                               (dom/option #js {:value id} title))))))

(def tags-select (om/factory TagsSelect))


(defui TasksList
       Object
       (render [this]
               (let [{:keys [tasks/all-tasks]} (om/props this)]
                 (apply dom/div #js {}
                        (for [task all-tasks]
                          (task-ui task))))))


(def task-list (om/factory TasksList))


(defui App
       static om/IQuery
       (query [this]
              `[{:tasks/all-tasks ~(om/get-query TaskView)}
                {:tags/all-tags ~(om/get-query TagView)}]
              #_(vec
                (concat
                 (om/get-query TasksList)
                 (om/get-query TagsSelect))))               ;When we construct manually the union, it breaks mutation
       Object
       (render [this]
               (dom/div #js {}
                        (task-list (om/props this))
                        (tags-select (om/props this)))))

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



(defmethod read :tags/all-tags
  [{:keys [state selector]} _ _]
  {:value (d/q '[:find [(pull ?e ?selector) ...]
                 :in $ ?selector
                 :where
                 [?e :tag/title]]
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

(om/add-root! reconciler App (gdom/getElement "app"))



