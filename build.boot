(set-env! :resource-paths #{"input"})

(require '[boot.util :as util]
         '[clojure.java.io :as io])

(def fs-stats (atom {:deletions 0
                     :hard-links 0
                     :copy 0}))

(deftask print-fs-stats []
  (with-pre-wrap fs
    (util/info "FS Actions %s" (with-out-str (clojure.pprint/pprint @fs-stats)))
    (util/info "Fileset count %s\n" (count (vals (:tree fs))))
    (reset! fs-stats {:deletions 0 :hard-links 0 :copy 0})
    fs))

(alter-var-root
 (var boot.file/sync!)
 (fn [_]
   (fn sync! [pred dest & srcs]
     (let [before (boot.file/tree-for dest)
           after  (apply boot.file/tree-for srcs)]
       (doseq [[op p x] (boot.file/patch pred before after)]
         ;; (util/dbug "New OP: %s\n" [op p x])
         (case op
           :rm (boot.file/delete-file x)
           :cp (boot.file/copy-with-lastmod x (clojure.java.io/file dest p))))))))

(alter-var-root
 (var boot.file/copy-with-lastmod)
 (fn [_]
   (fn copy-with-lastmod
     [src-file dst-file]
     (let [src-last-mod (.lastModified src-file)
           dst-par      (.getParent dst-file)
           delete-dbg   #(util/dbug "Deleting %s\nsrc last modified %s\ndst last modified %s\n"
                                     dst-file      src-last-mod          (.lastModified dst-file))]
       (clojure.java.io/make-parents dst-file)
       ;; (when-not (.canWrite (clojure.java.io/file dst-par))
       ;;   (throw (ex-info (format "Can't write to directory (%s)." dst-par) {:dir dst-par})))
       (if boot.file/*hard-link*
         (do
           (when (and (.exists dst-file)
                      ;; If src-file is younger than dst-file
                      (<= src-last-mod (.lastModified dst-file)))
             (delete-dbg)
             (swap! fs-stats update :deletions inc)
             (.delete dst-file))
           (when-not (.exists dst-file)
             (swap! fs-stats update :hard-links inc)
             (boot.file/hard-link src-file dst-file)))
         (do
           (when (.exists dst-file)
             (delete-dbg)
             (swap! fs-stats update :deletions inc)
             (.delete dst-file))
           (swap! fs-stats update :copy inc)
           (clojure.java.io/copy src-file dst-file)
           (.setLastModified dst-file src-last-mod)))))))

(deftask modify-random
  [n number I int  "Number of files to modify"
   m modify   bool "Modify file(s) in fs"]
  (with-pre-wrap fileset
    (if modify
      (let [t (tmp-dir!)]
        (dotimes [_ (or number 1)]
          (let [f (rand-nth (vals (:tree fileset)))
                target (io/file t (:path f))]
            (util/info "Modifying file %s\n" (:path f))
            (io/make-parents target)
            (spit target (str (slurp (tmp-file f)) (.getName target)))))
        (-> fileset (add-resource t) commit!))
      fileset)))
