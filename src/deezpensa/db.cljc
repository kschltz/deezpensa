(ns deezpensa.db
  (:require
    ["dart:async" :as async]
    ["package:path/path.dart" :as p]
    ["package:sqflite/sqflite.dart" :as sql]
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
  (sql_ffi/sqfliteFfiInit)
  (set! sql/databaseFactory sql_ffi/databaseFactoryFfi)

  (sql/openDatabase
    (p/join (await (sql/getDatabasesPath)) db-name)
    .version version
    .onCreate (fn [db version]
                (.execute db
                          "CREATE TABLE if not exists items(id text, name TEXT, section TEXT)")
                nil)))


(defn insert [db table value]
  (.insert (await db)
           table
           (to-map value)
           .conflictAlgorithm sql/ConflictAlgorithm.replace))


(defn list [db table]
  (.query (await db) table))