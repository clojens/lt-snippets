(ns lt.plugins.lt-snippets.snippets
  (:require [lt.object :as object]
            [lt.objs.files :as files]
            [lt.objs.keyboard :as keyboard]
            [clojure.string :as s])
  (:require-macros [lt.macros :refer [defui behavior]]))


(def snippet-dir (files/lt-user-dir "snippets"))


(defn load-if-exists [path file]
  (let [fullpath (files/join path file)]
    (if-not (files/exists? fullpath)
      fullpath
      (files/bomless-read fullpath))))

(defn load-files [path snipgroup]
  {:mode (:mode snipgroup)
   :snippets (map (fn [item]
                    (if-let [file (:snippet-file item)]
                      (assoc item :snippet (load-if-exists path file))
                      item))
                  (:snippets snipgroup))})

(defn load-one [path]
  (->
   (files/bomless-read path)
   (cljs.reader/read-string)
   ((partial load-files (files/parent path)))))


(defn load-all []
  (->
   (files/filter-walk (fn [path] (= (files/ext path) "edn")) snippet-dir)
   ((partial map load-one))))


(defn all []
  (load-all))


(defn degroup [snippets]
  (mapcat identity (map (fn [snip] (:snippets snip)) snippets)))

(defn by-key [key snippets]
  (filter #(= key (:key %)) (degroup snippets)))


(defn by
  ([key snippets]
   (by-key key snippets))
  ([key modes snippets]
   (by-key key (filter #(contains? modes (keyword (:mode %))) snippets))))



(defn get-shortcuts []
  (mapcat identity (map (fn [keygroup]
                         (map (fn [km]
                                (hash-map :tag (first keygroup)
                                          :shortcut (first km)
                                          :key (last (first (first (rest km))))))
                              (filter #(.contains (str %) ":snippet.by_key") (first (rest keygroup)))))
                       (seq @keyboard/keys))))


(defn all-keymapped []
  (let [snippets (degroup (all))
        shortcuts (get-shortcuts)]
    (map (fn [snip]
           (assoc snip :shortcut (when-let [sc (some #(if (= (:key snip) (:key %)) %)  shortcuts)] (:shortcut sc))))
         snippets)))


(defn get-tabstops[snippet]
  (into #{} (filter #(not (= % "$0")) (re-seq #"\$\d+" snippet))))

(defn tabstops? [snippet]
  (empty? (get-tabstops snippet)))
