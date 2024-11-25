(ns deezpensa.db
  (:require
    ["dart:io" :as io]
    ["dart:async" :as async]
    ["package:path/path.dart" :as p]
    ["package:sqflite/sqflite.dart" :as sql]
    ["package:sqflite_common_ffi_web/sqflite_ffi_web.dart" :as sql_ffi_web]
    ["package:sqflite_common_ffi/sqflite_ffi.dart" :as sql_ffi]))

(defn to-map [amap]
  (let [^#/(Map String Object?) m (-> (update-keys amap name)
                                      (update-vals (fn [v]
                                                     (if (uuid? v)
                                                       (str v)
                                                       v))))]
    m))




(defn start [db-name & {:keys [version]
                        :or {version 1}}]

  (prn {:linux? io/Platform.isLinux
        :android? io/Platform.isAndroid})
  (cond
    io/Platform.isLinux
    (do
      (prn "Start linux sqlite")
      (sql_ffi/sqfliteFfiInit)
      (set! sql/databaseFactory sql_ffi/databaseFactoryFfi)
      (.then (sql/getDatabasesPath)
             (fn [path]
               (prn "Databases path: " path)))))

  (sql/openDatabase
    (p/join (await (sql/getDatabasesPath)) db-name)
    .version version
    .onCreate (fn [db version]
                (.execute db
                          "CREATE TABLE if not exists items(id text, name TEXT, section TEXT)")
                nil)))


(defn insert [db table value]
  (await (.insert (await db)
                  table
                  (to-map value)
                  .conflictAlgorithm sql/ConflictAlgorithm.replace)))

(defn batch-insert [db table values]
  (let [batch (.batch (await db))
        values (mapv to-map values)]
    (run!
      (fn [v] (.insert batch table v .conflictAlgorithm sql/ConflictAlgorithm.replace))
      values)
    (await (.commit batch))))


(defn list [db table & {:keys [order-by]
                        :or {order-by :id}}]
  (.then
    (.query (await db) table)
    (fn [rows]
      (->> rows
           (map (fn [^#/(Map String, Object?) r]
                  (zipmap (map keyword (keys r))
                          (vals r))))
           (sort-by order-by)))))