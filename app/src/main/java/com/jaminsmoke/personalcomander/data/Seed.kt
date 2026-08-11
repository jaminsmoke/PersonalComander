package com.jaminsmoke.personalcomander.data

object Seed {

    // Layout constants for the free-form board (in dp)
    private const val COL_SPACING = 140f
    private const val ROW_SPACING = 160f

    fun mesas(): List<Mesa> {
        // Arrange mesas in a grid: 4 columns per row, grouped by zone
        // Row 0: Terraza (round, 2p) — 4 mesas
        // Row 1: Interior cuadradas (square, 4p) — 4 mesas
        // Row 2: Interior rectangulares (rectangular, 8p) — 4 mesas
        // Row 3: Interior XL — 2 mesas
        // Row 4: Barra (round, 3p) — 2 mesas

        val mesas = mutableListOf<Mesa>()

        // Row 0 — Terraza
        for (i in 0..3) {
            mesas.add(Mesa(
                numero = 1 + i,
                forma = MesaForma.REDONDA, zona = "Terraza", capacidad = 2,
                posX = i * COL_SPACING, posY = 0f
            ))
        }
        // Row 1 — Interior cuadradas
        for (i in 0..3) {
            mesas.add(Mesa(
                numero = 5 + i,
                forma = MesaForma.CUADRADA, zona = "Interior", capacidad = 4,
                posX = i * COL_SPACING, posY = ROW_SPACING
            ))
        }
        // Row 2 — Interior rectangulares
        for (i in 0..3) {
            mesas.add(Mesa(
                numero = 9 + i,
                forma = MesaForma.RECTANGULAR, zona = "Interior", capacidad = 8,
                posX = i * COL_SPACING, posY = 2 * ROW_SPACING
            ))
        }
        // Row 3 — Interior XL (only 2, centered)
        mesas.add(Mesa(
            numero = 13,
            forma = MesaForma.RECTANGULAR_XL, zona = "Interior", capacidad = 12,
            posX = 0f, posY = 3 * ROW_SPACING
        ))
        mesas.add(Mesa(
            numero = 14,
            forma = MesaForma.RECTANGULAR_XL, zona = "Interior", capacidad = 12,
            posX = COL_SPACING, posY = 3 * ROW_SPACING
        ))
        // Row 4 — Barra (only 2)
        mesas.add(Mesa(
            numero = 15,
            forma = MesaForma.REDONDA, zona = "Barra", capacidad = 3,
            posX = 0f, posY = 4 * ROW_SPACING
        ))
        mesas.add(Mesa(
            numero = 16,
            forma = MesaForma.REDONDA, zona = "Barra", capacidad = 3,
            posX = COL_SPACING, posY = 4 * ROW_SPACING
        ))

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
