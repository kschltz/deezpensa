(ns deezpensa.core
  (:require ["package:flutter/material.dart" :as m]
            [cljd.flutter :as f]))




(defn dispensa-page []
  (m/Center
    .child (m/Text "Página da Dispensa"
                   .style (m/TextStyle .fontSize 24))))

(defn lista-de-compras-page []
  (m/Center
    .child (m/Text "Página da Lista de Compras"
                   .style (m/TextStyle .fontSize 24))))

(defn drawer-header []
  (m/DrawerHeader
    .decoration (m/BoxDecoration .color m/Colors.blue)
    .child (m/Text "Menu"
                   .style (m/TextStyle
                            .color m/Colors.white
                            .fontSize 24))))

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

(defn gestor-domestico []
  (let [selected-page (atom 0)
        set-page (fn [page]
                   (prn page)
                   (reset! selected-page page)
                   nil)
        pages [(dispensa-page) (lista-de-compras-page)]]
    (f/widget
      :watch [page selected-page]
      (m/Scaffold
        .appBar (m/AppBar .title (m/Text (str "Gestor Doméstico: " page)))
        .drawer (drawer set-page)
        ))))
;
;(defn main []
;  (m/runApp
;    (m/MaterialApp
;      {.title "Gestor Doméstico"
;       .theme (m/ThemeData {.primarySwatch m/Colors.blue})
;       .home (gestor-domestico)})))

(defn main []
  (f/run
    (m/MaterialApp
      .title "Welcome to Flutter"
      .theme (m/ThemeData .primarySwatch m.Colors/pink))
    .home (gestor-domestico)))
