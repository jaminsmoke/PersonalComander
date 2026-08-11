package com.jaminsmoke.personalcomander.data

object Seed {
    fun mesas(): List<Mesa> = (1..16).map { Mesa(numero = it) }

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
