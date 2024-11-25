(ns deezpensa.core
  (:require ["package:flutter/material.dart" :as m]
            [clojure.string :as str]
            [deezpensa.db :as db]
            [deezpensa.model.item :as item]
            [cljd.flutter :as f]))

(def medium 18)
(def large 24)

(defn hover [widget]
  (let [hover (atom false)]
    (f/widget
      :watch [hovered? hover]
      (m/MouseRegion
        .onEnter (fn [_] (reset! hover true) nil)
        .onExit (fn [_] (reset! hover false) nil)
        .child (m/AnimatedContainer
                 .duration (dart:core/Duration .milliseconds 250)
                 .curve m/Curves.easeInOut,
                 .width (if hovered? 200 180),
                 .height (if hovered? 155 150),
                 .decoration (m/BoxDecoration
                               .boxShadow [(m/BoxShadow .color (if hovered?
                                                                 m/Colors.green
                                                                 m/Colors.white)
                                                        .blurRadius 3)])
                 .child widget)))))

(defn remove-from-stock [stock id]
  (swap! stock
         (fn [s]
           (println "removing" id)
           (remove #(= id (:id %)) s))))


(defn add-item-form [a-show?]
  (let [form-key (#/(m/GlobalKey m/FormState))]
    (f/widget
      :watch [show a-show?]
      (m/SizedBox
        .height (if show 200 0)
        .child(f/widget
                    :get [m/ScaffoldMessenger
                          :save-to-db]
                    :managed [name-ctrl (m/TextEditingController)
                              section-ctrl (m/TextEditingController)]
                    (m/Form .key form-key)
                    (m/Column .crossAxisAlignment m/CrossAxisAlignment.start)
                    .children
                    [(m/TextFormField
                       .controller name-ctrl
                       .decoration (m/InputDecoration .labelText "Produto")
                       .validator (fn [value] (when (str/blank? value) "campo obrigatório")))
                     (m/TextFormField
                       .controller section-ctrl
                       .decoration (m/InputDecoration .labelText "seção")
                       .validator (fn [value] (when (str/blank? value) "campo obrigatório")))
                     (m/Padding .padding (m/EdgeInsets.symmetric .vertical 16.0))
                     (m/Row
                       .mainAxisAlignment m/MainAxisAlignment.end
                       .children
                       [(m/ElevatedButton
                          .onPressed #(when (.validate (.-currentState form-key))
                                        (do
                                          (swap! a-show? not)
                                          (save-to-db {:id (str (random-uuid))
                                                       :name (.text name-ctrl)
                                                       :section (.text section-ctrl)})
                                          (.showSnackBar scaffold-messenger
                                                         (f/widget
                                                           (m/SnackBar
                                                             .content
                                                             (m/Text (str "Salvando dados..."))))))
                                        nil)
                          .child (m/Text "Adicionar"))
                        (m/ElevatedButton
                         .onPressed (fn [] (swap! a-show? not))
                         .child (m/Text "Cancelar"))
                        (m/SizedBox .width 16.0)
                     ])])))))

(defn stock-table [{:keys [title stock]}]

  (let [a-show? (atom false)
        rows (->> stock
                  (map
                    (fn [{:keys [id name section]}]
                      (hover (m/Card
                               .child (m/Column
                                        .mainAxisSize m/MainAxisSize.min
                                        .children [(m/ListTile
                                                     .title (m/Text name .style (m/TextStyle .fontSize medium))
                                                     .subtitle (m/Text section .style (m/TextStyle .fontSize medium)))
                                                   (m/Row
                                                     .mainAxisAlignment m/MainAxisAlignment.end
                                                     .children [;;send to shopping list
                                                                (m/IconButton
                                                                  .icon (m/Icon m/Icons.add_shopping_cart)
                                                                  .onPressed (fn [] (print "send to shopping list")))
                                                                ;;remove
                                                                (f/widget
                                                                  :get [:remove-by-id]
                                                                  (m/IconButton
                                                                    .icon (m/Icon m/Icons.delete)
                                                                    .onPressed (fn []
                                                                                 (print "remove")
                                                                                 (remove-by-id id)

                                                                                 nil)))
                                                                ])]))))))]

    (f/widget
      :bind {:a-show? a-show?}
      :watch [show? a-show?]
      (m/Center
        .child (m/ListView
                 .children
                 (concat [(m/Row
                            .mainAxisAlignment m/MainAxisAlignment.start
                            .children [(m/Text title .style (m/TextStyle .fontSize large))

                                       (m/Divider)
                                       (m/Card
                                         .child (m/IconButton
                                                  .icon (m/Icon m/Icons.add)
                                                  .onPressed (fn []
                                                               (swap! a-show? not)
                                                               (print "add"))))])
                          (m/Divider)
                          (add-item-form a-show?)
                          (m/Divider)]
                         rows)))))

  )

(defn despensa-page [{:keys [stock]}]
  (f/widget
    :watch [s stock]
    :bind {:save-to-db (fn [item] (swap! stock conj item) nil)
           :remove-by-id (fn [id] (remove-from-stock stock id))}
    (m/Center
      .child (stock-table {:title "Despensa" :stock s :stock-atom stock}))))

(defn lista-de-compras-page []
  (m/Center
    .child (m/Text "Lista de Compras"
                   .style (m/TextStyle .fontSize large))))


(defn menu-item [icon title on-tap]
  (m/ListTile
    .leading (m/Icon icon)
    .title (m/Text title)
    .onTap (fn []
             (print on-tap)
             (on-tap))))

(defn drawer [set-page]
  (m/Drawer
    .child (m/ListView
             .padding m/EdgeInsets.zero
             .children [(menu-item m/Icons.kitchen "Despensa" (partial set-page 0))
                        (menu-item m/Icons.add_shopping_cart "Lista de Compras" (partial set-page 1))])))

(defonce mock-stock
         [{:id "a" :name "Arroz" :section "Grãos"}
          {:id "b" :name "Feijão" :section "Grãos"}
          {:id "c" :name "Macarrão" :section "Massas"}
          {:id "d" :name "Molho de Tomate" :section "Molhos"}])

(defn gestor-domestico []
  (let [database (db/start "deezpensa.db")
        despensa (atom [])
        selected-page (atom 0)
        set-page (fn [page]
                   (reset! selected-page page)
                   nil)
        pages [(despensa-page {:stock despensa}) (lista-de-compras-page)]
        refresh (fn [] (.then (db/list database "items")
                              (fn [i] (reset! despensa (distinct i)))))]

    (refresh)
    (add-watch despensa :update
               (fn [_ _ _ new-despensa]
                 (db/batch-insert database "items" new-despensa) nil))

    (f/widget
      :watch [page selected-page]
      (m/Scaffold
        .appBar (m/AppBar .title (m/Text (str "Gestor Doméstico: " page)))
        .drawer (drawer set-page)
        .body (m/Padding
                .padding (m/EdgeInsets.all 36)
                .child (get pages page))))))


(defn main []
  (f/run
    (m/MaterialApp
      .title "Deezpensa"
      .theme (m/ThemeData .primarySwatch m.Colors/pink))
    .home (gestor-domestico)))
