(ns todomvc.todo-items-views
  (:require [cljs.pprint :as pp]
            [goog.events.Event :as event]
            [todomvc.todo :refer [td-completed td-created
                                  td-delete!] :as todo]
            [todomvc.todo-view :refer [todo-list-item]]
            [tiltontec.matrix.api :refer [cF]]
            [tiltontec.model.core :refer [kid-values-kids matrix mget mpar
                                          mset! mxu-find-type] :as md]
            [tiltontec.util.core :as util]
            [tiltontec.web-mx.api :refer [a button div footer input label li
                                          section span ul]]))

;; --- convenient accessors ---------------------
;;
;; One point of friction while coding with Matrix is
;; navigating the Matrix to information we need to
;; code up any given functionality.
;;
;; In this case, navigation is by the Matrix pseudo-tyoe,
;; handled by the function 'mxu-find-type'.
;;
;; Matrix includes quite a menagerie of utilities
;; to find other nodes in the Matrix, akin to various
;; ways of authoring CSS selectors.
;;
;; Would a one-atom DB approach work? Not really, for reasons
;; involving the Matrix life-cycle, an advanced topic we would
;; like to defer.

(defn mx-find-matrix [mx]
  (assert mx)
  (mxu-find-type mx :todoApp))

;; Unsurprisingly, the state of the to-dos themselves
;; drives most of the TodoMVC dynamic behavior being exercised.
;; Below we wrap up navigation to that data structure.

(defn mx-todos
  "Given a node in the matrix, navigate to the root and read the todos. After
  the matrix is initially loaded (say in an event handler), one can pass nil
  and find the matrix in @matrix. Put another way, a starting node is required
  during the matrix's initial build."
  ([]
   (mget @matrix :todos))

  ([mx]
   (if (nil? mx)
     (mx-todos)
     (let [mtrx (mx-find-matrix mx)]
       (assert mtrx)
       (mget mtrx :todos)))))

(defn mx-todo-items
  ([]
   (mx-todo-items nil))
  ([mx]
   (mget (mx-todos mx) :items)))

(defn mx-route [mx]
  (mget (mx-find-matrix mx) :route))

;;; --- toggle all component -----------------------------------------

(defn toggle-all []
  (div {} {;; 'action' is an ad hoc bit of intermediate state that will be used to decide the
           ;; input HTML checked attribute and will also guide the label onclick handler.
           :action (cF (if (every? td-completed (mx-todo-items me))
                         :uncomplete :complete))}
       (input {:id        "toggle-all"
               :class     "toggle-all"
               :type "checkbox"
               :checked   (cF (= (mget (mpar me) :action) :uncomplete))})
       (label {:for     "toggle-all"
               :onclick #(let [action (mget me :action)]
                        ;; NB! this ^^ me is a lexical anaphor supplied by the `cFkids` formula
                        ;; macro that invisibly wraps all Web/MX component children. `me` is akin
                        ;; to Smalltalk `self` or JS `this`, and all the cF macros supply it.
                        ;; Since the `label` is a child of the DIV, me is bound to the DIV, where the
                        ;; desired :action lives. This is a unique situation in that, in a handler, we usually
                        ;; want a reference to the object owning a handler. We could achieve
                        ;; that by simply wrapping the handler in `cF`. It will run just once
                        ;; and then be optimized away, so there is no memory or performance hit, but we
                        ;; would get the desired `me` (in a different situation -- here we are happy
                        ;; to have a reference to the DIV).
                        ;;
                        ;; preventDefault else browser messes with checked, which _we_ handle
                           (event/preventDefault %)
                           (doseq [td (mx-todo-items)]
                             (mset! td :completed (when (= action :complete) (util/now)))))}
              "Mark all as complete")))

;;; --- views --------------------------------------------------------

(defn todo-items-list []
  (section {:class "main"}
           (toggle-all)
           (ul {:class "todo-list"}
               {:kid-values (cF (when-let [rte (mx-route me)]
                                  (sort-by td-created
                                           (mget (mx-todos me)
                                                 (case rte
                                                   "All" :items
                                                   "Completed" :items-completed
                                                   "Active" :items-active)))))
                :kid-key #(mget % :todo)
                :kid-factory (fn [me todo]
                               (todo-list-item todo))}
      ;; _cache is prior value for this implicit 'kids' slot; k-v-k uses it for diffing
               (kid-values-kids me _cache))))

(defn todo-items-dashboard []
  (footer {:class  "footer"
           :hidden (cF (mget (mx-todos me) :empty?))}

    ;; Items remaining
          (span {:class   "todo-count"
                 :content (cF (pp/cl-format nil "<strong>~a</strong>  item~:P remaining"
                                            (count (mget (mx-todos me) :items-active))))})

    ;; Item filters
          (ul {:class "filters"}
              (for [[label route] [["All", "#/"]
                                   ["Active", "#/active"]
                                   ["Completed", "#/completed"]]]
                (li {} (a {:href     route
                           :selector label
                           :class    (cF (when (= (:selector @me) (mx-route me))
                                           "selected"))}
                          label))))

          (button {:class   "clear-completed"
                   :hidden  (cF (empty? (mget (mx-todos me) :items-completed)))
                   :onclick #(doseq [td (mget (mx-todos me) :items-completed)]
                               (td-delete! td))}
                  "Clear completed")))