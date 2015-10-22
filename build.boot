(set-env! :resource-paths #{"input"})


(require '[boot.util :as u]
         '[clojure.java.io :as io])

(deftask modify-random
  [n number I int  "Number of files to modify"
   m modify M bool "Modify or just list numer of files in fs"]
  (with-pre-wrap fileset
    (u/info "%s files in fileset\n" (count (:tree fileset)))
    (if modify
      (let [f (rand-nth (vals (:tree fileset)))
            t (tmp-dir!)]
        (u/info "Modifying file %s\n" (:path f))
        (spit (io/file t (:path f))
              (str (slurp (tmp-file f)) "xxx"))
        (-> fileset (add-resource t) commit!))
      fileset)))
