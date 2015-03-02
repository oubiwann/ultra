(ns ultra.plugin
  (:require [leiningen.core.project :as project]
            [leiningen.test]
            [robert.hooke :refer [add-hook]]
            [ultra.hardcore]
            [whidbey.plugin :as plugin]))

(defn prepend-repl-middleware
  "Add our nREPL middleware to the front of the nREPL middleware vector.

  This is to avoid a sneaky bug with Whidbey and CIDER that's caused by
  load order."
  {:added "0.2.1"}
  [project]
  (let [current-middleware (or (-> project
                                   :repl-options
                                   :nrepl-middleware) [])
        new-middleware (reduce conj
                               [`clojure.tools.nrepl.middleware.render-values/render-values]
                               current-middleware)]
    (assoc-in project [:repl-options :nrepl-middleware] new-middleware)))

(defn add-repl-middleware
  "Check to see if we need to add nREPL middleware, and if so, add it."
  {:added "0.2.0"}
  [project {:keys [repl] :as opts}]
  (if (not (false? repl))
    (-> project
        (update-in [:repl-options] merge
                   {:nrepl-context
                    {:interactive-eval
                     `{:renderer whidbey.render/render-str}}})
        prepend-repl-middleware)
    project))

(defn inject-repl-initialization
  "Move most configuration into REPL initialization."
  {:added "0.3.0"}
  [project opts]
  (assoc-in project
            [:repl-options :init]
            `(do (require 'ultra.hardcore)
                (ultra.hardcore/configure! ~opts))))

(defn add-ultra
  "Add ultra as a project dependency and inject configuration."
  {:added "0.1.0"}
  [project opts]
  (-> project
      (update-in [:dependencies] concat
                 `[[mvxcvi/puget "0.7.1"]
                   [mvxcvi/whidbey "0.5.0"]
                   [venantius/ultra "0.3.2"]
                   [im.chit/hara.class "2.1.8"]
                   [im.chit/hara.reflect "2.1.8"]])
      (update-in [:injections] concat `[(require 'ultra.hardcore) 
                                        (ultra.hardcore/add-test-hooks! ~opts)])
      (assoc :monkeypatch-clojure-test false)
      (add-repl-middleware opts)
      (inject-repl-initialization opts)))

(defn middleware
  "Ultra's middleware re-writes the project map."
  {:added "0.1.0"}
  [project]
  (let [opts (-> (:ultra project)
                 (assoc :print-meta false
                        :map-delimiter ""
                        :print-fallback :print
                        :sort-keys true))]
    (add-ultra project opts)))
