(ns deezpensa.core
  (:require ["package:flutter/material.dart" :as m]
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

(defn stock-table [{:keys [title stock]}]
  (let [rows (->> stock
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
                                                                (m/IconButton
                                                                  .icon (m/Icon m/Icons.delete)
                                                                  .onPressed (fn [] (print "remove")))
                                                                ])]))))))]

    (m/Center
      .child (m/ListView
               .children (concat [(m/Text title .style (m/TextStyle .fontSize large))
                                  (m/Divider)]
                                 rows))))

  )

(defn dispensa-page [{:keys [stock]}]
  (f/widget
    :watch [s stock]
    (m/Center
      .child (stock-table {:title "Dispensa" :stock s}))))

(defn lista-de-compras-page []
  (m/Center
    .child (m/Text "Lista de Compras"
                   .style (m/TextStyle .fontSize large))))

(defn drawer-header []
  (m/DrawerHeader
    .decoration (m/BoxDecoration .color m/Colors.blue)
    .child (m/Text "Menu"
                   .style (m/TextStyle
                            .color m/Colors.white
                            .fontSize large))))

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
             ;(drawer-header)
             .children [(menu-item m/Icons.kitchen "Dispensa" (partial set-page 0))
                        (menu-item m/Icons.add_shopping_cart "Lista de Compras" (partial set-page 1))])))

(defonce mock-stock
         [{:id (random-uuid) :name "Arroz" :section "Grãos"}
          {:id (random-uuid) :name "Feijão" :section "Grãos"}
          {:id (random-uuid) :name "Macarrão" :section "Massas"}
          {:id (random-uuid) :name "Molho de Tomate" :section "Molhos"}
          {:id (random-uuid) :name "Óleo" :section "Óleos"}
          {:id (random-uuid) :name "Sal" :section "Temperos"}
          {:id (random-uuid) :name "Açúcar" :section "Temperos"}
          {:id (random-uuid) :name "Café" :section "Bebidas"}
          {:id (random-uuid) :name "Leite" :section "Bebidas"}
          {:id (random-uuid) :name "Refrigerante" :section "Bebidas"}
          {:id (random-uuid) :name "Cerveja" :section "Bebidas"}
          {:id (random-uuid) :name "Carne" :section "Carnes"}
          {:id (random-uuid) :name "Frango" :section "Carnes"}
          {:id (random-uuid) :name "Peixe" :section "Carnes"}
          {:id (random-uuid) :name "Ovos" :section "Carnes"}
          {:id (random-uuid) :name "Pão" :section "Padaria"}
          {:id (random-uuid) :name "Bolacha" :section "Padaria"}
          {:id (random-uuid) :name "Biscoito" :section "Padaria"}
          {:id (random-uuid) :name "Bolo" :section "Padaria"}
          {:id (random-uuid) :name "Leite Condensado" :section "Doces"}
          {:id (random-uuid) :name "Chocolate" :section "Doces"}
          {:id (random-uuid) :name "Gelatina" :section "Doces"}
          {:id (random-uuid) :name "Sorvete" :section "Doces"}
          {:id (random-uuid) :name "Detergente" :section "Limpeza"}
          {:id (random-uuid) :name "Sabão em Pó" :section "Limpeza"}
          {:id (random-uuid) :name "Desinfetante" :section "Limpeza"}
          {:id (random-uuid) :name "Água Sanitária" :section "Limpeza"}
          {:id (random-uuid) :name "Papel Higiênico" :section "Limpeza"}
          {:id (random-uuid) :name "Sabonete" :section "Higiene"}
          {:id (random-uuid) :name "Shampoo" :section "Higiene"}
          {:id (random-uuid) :name "Condicionador" :section "Higiene"}
          {:id (random-uuid) :name "Creme Dental" :section "Higiene"}
          {:id (random-uuid) :name "Fio Dental" :section "Higiene"}
          {:id (random-uuid) :name "Desodorante" :section "Higiene"}])

(defn gestor-domestico []
  (let [database (db/start "deezpensa.db")
        _ (db/insert database "items" (item/map->Item {:id (random-uuid) :name "Arroz" :section "Grãos"}))
        items (db/list database "items")
        _ (.then items (fn [x] (prn "ITEMS: " x)))
        selected-page (atom 0)
        stock (atom mock-stock)
        set-page (fn [page]
                   (reset! selected-page page)
                   nil)
        pages [(dispensa-page {:stock stock}) (lista-de-compras-page)]]
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
