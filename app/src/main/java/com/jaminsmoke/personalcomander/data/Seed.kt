package com.jaminsmoke.personalcomander.data

object Seed {

    // Layout constants for the free-form board (in dp)
    // Cell = 40dp; mesa = 120dp → 40dp gap between cards
    private const val CELL = 40f
    private const val COL_SPACING = 160f   // 4 cells per column
    private const val ROW_SPACING = 160f   // 4 cells per row
    private const val MARGIN = 40f         // margin from board edge

    fun mesas(): List<Mesa> {
        // Arrange mesas in a grid: 4 columns per row, grouped by zone
        // Row 0: Terraza (round, 2p) — 4 mesas
        // Row 1: Interior cuadradas (square, 4p) — 4 mesas
        // Row 2: Interior rectangulares (rectangular, 8p) — 4 mesas
        // Row 3: Interior XL — 2 mesas
        // Row 4: Barra (round, 3p) — 2 mesas

        val mesas = mutableListOf<Mesa>()
        val indicePorZona = mutableMapOf<String, Int>()

        fun add(numero: Int, forma: MesaForma, zona: String, capacidad: Int, posX: Float, posY: Float) {
            val indice = (indicePorZona[zona] ?: 0) + 1
            indicePorZona[zona] = indice
            mesas.add(Mesa(
                numero = numero, forma = forma, zona = zona, capacidad = capacidad,
                posX = posX, posY = posY, indiceZona = indice
            ))
        }

        // Row 0 — Terraza (round, 2p): T1–T4
        for (i in 0..3) {
            add(1 + i, MesaForma.REDONDA, "Terraza", 2, MARGIN + i * COL_SPACING, MARGIN)
        }
        // Row 1 — Interior cuadradas (square, 4p): I1–I4
        for (i in 0..3) {
            add(5 + i, MesaForma.CUADRADA, "Interior", 4, MARGIN + i * COL_SPACING, MARGIN + ROW_SPACING)
        }
        // Row 2 — Interior rectangulares (rect, 8p): I5–I8
        for (i in 0..3) {
            add(9 + i, MesaForma.RECTANGULAR, "Interior", 8, MARGIN + i * COL_SPACING, MARGIN + 2 * ROW_SPACING)
        }
        // Row 3 — Interior XL (only 2): I9–I10
        add(13, MesaForma.RECTANGULAR_XL, "Interior", 12, MARGIN, MARGIN + 3 * ROW_SPACING)
        add(14, MesaForma.RECTANGULAR_XL, "Interior", 12, MARGIN + COL_SPACING, MARGIN + 3 * ROW_SPACING)
        // Row 4 — Barra (round, 3p): B1–B2
        add(15, MesaForma.REDONDA, "Barra", 3, MARGIN, MARGIN + 4 * ROW_SPACING)
        add(16, MesaForma.REDONDA, "Barra", 3, MARGIN + COL_SPACING, MARGIN + 4 * ROW_SPACING)

        return mesas
    }

    fun productos(): List<Producto> = listOf(
        Producto(nombre = "Pan con tomate", categoria = "Entrantes", precio = 3.50),
        Producto(nombre = "Ensaladilla rusa", categoria = "Entrantes", precio = 5.00),
        Producto(nombre = "Patatas bravas", categoria = "Entrantes", precio = 4.50),
        Producto(nombre = "Croquetas caseras", categoria = "Entrantes", precio = 6.00),
        Producto(nombre = "Ensalada César", categoria = "Ensaladas", precio = 7.50),
        Producto(nombre = "Ensalada de la casa", categoria = "Ensaladas", precio = 6.50),
        Producto(nombre = "Pizza Margarita", categoria = "Pizzas", precio = 9.50),
        Producto(nombre = "Pizza Barbacoa", categoria = "Pizzas", precio = 11.00),
        Producto(nombre = "Pizza Cuatro Quesos", categoria = "Pizzas", precio = 11.50),
        Producto(nombre = "Hamburguesa Clásica", categoria = "Burgers", precio = 8.00),
        Producto(nombre = "Hamburguesa Completa", categoria = "Burgers", precio = 9.50),
        Producto(nombre = "Hamburguesa con Queso", categoria = "Burgers", precio = 8.50),
        Producto(nombre = "Entrecot a la brasa", categoria = "Carnes", precio = 16.00),
        Producto(nombre = "Solomillo de cerdo", categoria = "Carnes", precio = 12.50),
        Producto(nombre = "Pollo asado", categoria = "Carnes", precio = 10.00),
        Producto(nombre = "Bacalao a la plancha", categoria = "Pescados", precio = 14.00),
        Producto(nombre = "Merluza rebozada", categoria = "Pescados", precio = 12.00),
        Producto(nombre = "Agua mineral", categoria = "Bebidas", precio = 2.00),
        Producto(nombre = "Refresco", categoria = "Bebidas", precio = 2.50),
        Producto(nombre = "Cerveza caña", categoria = "Bebidas", precio = 2.50),
        Producto(nombre = "Vino de la casa (copa)", categoria = "Bebidas", precio = 3.00),
        Producto(nombre = "Tinto de verano", categoria = "Bebidas", precio = 3.00),
        Producto(nombre = "Café solo", categoria = "Bebidas", precio = 1.50),
        Producto(nombre = "Café con leche", categoria = "Bebidas", precio = 1.80),
        Producto(nombre = "Tarta de queso", categoria = "Postres", precio = 4.50),
        Producto(nombre = "Flan de huevo", categoria = "Postres", precio = 3.50),
        Producto(nombre = "Helado (2 bolas)", categoria = "Postres", precio = 3.00)
    )
}
