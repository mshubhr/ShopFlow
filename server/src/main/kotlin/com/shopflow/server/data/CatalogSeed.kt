package com.shopflow.server.data

import com.shopflow.server.models.ServerProductDto

object CatalogSeed {
    val products = listOf(
        ServerProductDto(
            id = 1,
            title = "Essence Mascara Lash Princess",
            description = "The Essence Mascara Lash Princess is a popular and cruelty-free formula designed to give dramatic volume and sculpted length.",
            price = 9.99,
            rating = 4.94,
            category = "beauty",
            brand = "Essence",
            stock = 15,
            thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Essence%20Mascara%20Lash%20Princess/thumbnail.png"
        ),
        ServerProductDto(
            id = 2,
            title = "Eyeshadow Palette with Mirror",
            description = "The Eyeshadow Palette with Mirror offers a versatile range of pigmented shades suitable for creating everyday to bold looks.",
            price = 19.99,
            rating = 4.35,
            category = "beauty",
            brand = "Glamour Beauty",
            stock = 8,
            thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Eyeshadow%20Palette%20with%20Mirror/thumbnail.png"
        ),
        ServerProductDto(
            id = 3,
            title = "Powder Canister",
            description = "Finely milled translucent setting powder that controls shine and provides a smooth matte finish all day.",
            price = 14.99,
            rating = 4.82,
            category = "beauty",
            brand = "Velvet Touch",
            stock = 25,
            thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Powder%20Canister/thumbnail.png"
        ),
        ServerProductDto(
            id = 4,
            title = "Red Lipstick Matte",
            description = "Classic bold red lipstick with a hydrating matte formula, non-drying and long-lasting pigmentation.",
            price = 12.99,
            rating = 4.51,
            category = "beauty",
            brand = "Chic Cosmetics",
            stock = 12,
            thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Red%20Lipstick/thumbnail.png"
        ),
        ServerProductDto(
            id = 5,
            title = "Chanel Coco Noir Eau De Parfum",
            description = "Coco Noir by Chanel is an intimate, sensual, and magnetic fragrance featuring luminous top notes and deep patchouli base notes.",
            price = 129.99,
            rating = 4.76,
            category = "fragrances",
            brand = "Chanel",
            stock = 6,
            thumbnail = "https://cdn.dummyjson.com/products/images/fragrances/Chanel%20Coco%20Noir%20Eau%20De%20Parfum/thumbnail.png"
        ),
        ServerProductDto(
            id = 6,
            title = "Dior J'adore",
            description = "An iconic floral bouquet with ylang-ylang, Damascus rose, and jasmine sambac. Epitome of feminine luxury.",
            price = 119.99,
            rating = 4.85,
            category = "fragrances",
            brand = "Dior",
            stock = 9,
            thumbnail = "https://cdn.dummyjson.com/products/images/fragrances/Dior%20J'adore/thumbnail.png"
        ),
        ServerProductDto(
            id = 7,
            title = "Gucci Bloom Eau de Parfum",
            description = "Capturing the spirit of contemporary, diverse, and authentic women with notes of tuberose and jasmine.",
            price = 99.99,
            rating = 4.62,
            category = "fragrances",
            brand = "Gucci",
            stock = 14,
            thumbnail = "https://cdn.dummyjson.com/products/images/fragrances/Gucci%20Bloom%20Eau%20de%20Parfum/thumbnail.png"
        ),
        ServerProductDto(
            id = 8,
            title = "Annibale Colombo Bed",
            description = "Handcrafted Italian luxury king-sized bed frame built from high-grade European walnut with upholstered headboard.",
            price = 1899.99,
            rating = 4.80,
            category = "furniture",
            brand = "Annibale Colombo",
            stock = 3,
            thumbnail = "https://cdn.dummyjson.com/products/images/furniture/Annibale%20Colombo%20Bed/thumbnail.png"
        ),
        ServerProductDto(
            id = 9,
            title = "Annibale Colombo Sofa",
            description = "Contemporary three-seater living room sofa with deep cushioned seats and stain-resistant fabric upholstery.",
            price = 2499.99,
            rating = 4.90,
            category = "furniture",
            brand = "Annibale Colombo",
            stock = 2,
            thumbnail = "https://cdn.dummyjson.com/products/images/furniture/Annibale%20Colombo%20Sofa/thumbnail.png"
        ),
        ServerProductDto(
            id = 10,
            title = "Bedside Table African Cherry",
            description = "Solid cherry wood nightstand with a silent glide drawer and open storage shelf for your bedside essentials.",
            price = 299.99,
            rating = 4.48,
            category = "furniture",
            brand = "Artisan Woodworks",
            stock = 7,
            thumbnail = "https://cdn.dummyjson.com/products/images/furniture/Bedside%20Table%20African%20Cherry/thumbnail.png"
        ),
        ServerProductDto(
            id = 11,
            title = "Crisp Red Apples",
            description = "Freshly harvested organic Red Delicious apples packed with natural antioxidants and dietary fiber.",
            price = 1.99,
            rating = 4.88,
            category = "groceries",
            brand = "Nature's Harvest",
            stock = 50,
            thumbnail = "https://cdn.dummyjson.com/products/images/groceries/Apple/thumbnail.png"
        ),
        ServerProductDto(
            id = 12,
            title = "Prime Beef Steak",
            description = "USDA Choice Angus beef sirloin steak, wet-aged for 21 days for maximum tenderness and rich flavor.",
            price = 12.99,
            rating = 4.75,
            category = "groceries",
            brand = "Butcher's Choice",
            stock = 18,
            thumbnail = "https://cdn.dummyjson.com/products/images/groceries/Beef%20Steak/thumbnail.png"
        ),
        ServerProductDto(
            id = 13,
            title = "Organic Whole Milk",
            description = "Grade-A pasture-raised whole milk, pasteurized and vitamin D enriched for superior creaminess and taste.",
            price = 3.49,
            rating = 4.60,
            category = "groceries",
            brand = "Dairy Gold",
            stock = 30,
            thumbnail = "https://cdn.dummyjson.com/products/images/groceries/Milk/thumbnail.png"
        ),
        ServerProductDto(
            id = 14,
            title = "Sourdough Artisan Bread",
            description = "Naturally fermented rustic sourdough loaf baked in stone hearth ovens with a crispy crust and airy crumb.",
            price = 4.29,
            rating = 4.82,
            category = "groceries",
            brand = "Bakers Hearth",
            stock = 22,
            thumbnail = "https://cdn.dummyjson.com/products/images/groceries/Bread/thumbnail.png"
        ),
        ServerProductDto(
            id = 15,
            title = "MacBook Pro 16 M3 Max",
            description = "Apple M3 Max chip with 16-core CPU, 40-core GPU, Liquid Retina XDR display, and up to 22 hours of battery life.",
            price = 3499.99,
            rating = 4.95,
            category = "laptops",
            brand = "Apple",
            stock = 5,
            thumbnail = "https://cdn.dummyjson.com/products/images/laptops/MacBook%20Pro%2014%20Inch%20Space%20Grey/thumbnail.png"
        ),
        ServerProductDto(
            id = 16,
            title = "Dell XPS 15 InfinityEdge",
            description = "Stunning 15.6-inch OLED touchscreen laptop with 13th Gen Intel Core i7, 32GB RAM, and NVIDIA RTX 4060 graphics.",
            price = 1999.99,
            rating = 4.68,
            category = "laptops",
            brand = "Dell",
            stock = 8,
            thumbnail = "https://cdn.dummyjson.com/products/images/laptops/Dell%20XPS%2013%209315/thumbnail.png"
        ),
        ServerProductDto(
            id = 17,
            title = "Lenovo ThinkPad X1 Carbon Gen 11",
            description = "Ultralight business flagship laptop engineered with carbon fiber weave, military-spec durability, and legendary keyboard.",
            price = 1749.99,
            rating = 4.72,
            category = "laptops",
            brand = "Lenovo",
            stock = 11,
            thumbnail = "https://cdn.dummyjson.com/products/images/laptops/Lenovo%20Yoga%209i/thumbnail.png"
        ),
        ServerProductDto(
            id = 18,
            title = "iPhone 15 Pro Max 256GB",
            description = "Aerospace-grade titanium design, A17 Pro chip, customizable Action button, and versatile 5x optical telephoto camera.",
            price = 1199.99,
            rating = 4.91,
            category = "smartphones",
            brand = "Apple",
            stock = 14,
            thumbnail = "https://cdn.dummyjson.com/products/images/smartphones/iPhone%2015%20Pro%20Max/thumbnail.png"
        ),
        ServerProductDto(
            id = 19,
            title = "Samsung Galaxy S24 Ultra",
            description = "Galaxy AI-powered flagship with 200MP camera, built-in S Pen, Titanium frame, and bright Dynamic AMOLED 2X display.",
            price = 1299.99,
            rating = 4.87,
            category = "smartphones",
            brand = "Samsung",
            stock = 10,
            thumbnail = "https://cdn.dummyjson.com/products/images/smartphones/Samsung%20Galaxy%20S24%20Ultra/thumbnail.png"
        ),
        ServerProductDto(
            id = 20,
            title = "Google Pixel 8 Pro",
            description = "Google Tensor G3 chip, best-in-class computational photography with Magic Eraser, and pristine 120Hz Super Actua display.",
            price = 999.99,
            rating = 4.79,
            category = "smartphones",
            brand = "Google",
            stock = 12,
            thumbnail = "https://cdn.dummyjson.com/products/images/smartphones/Realme%2012%20Pro%20Plus/thumbnail.png"
        ),
        ServerProductDto(
            id = 21,
            title = "Sony WH-1000XM5 Wireless Headphones",
            description = "Industry-leading noise canceling with two processors, 8 microphones, Auto NC Optimizer, and 30-hour battery life.",
            price = 399.99,
            rating = 4.89,
            category = "electronics",
            brand = "Sony",
            stock = 16,
            thumbnail = "https://cdn.dummyjson.com/products/images/mobile-accessories/Apple%20AirPods%20Max%20Silver/thumbnail.png"
        ),
        ServerProductDto(
            id = 22,
            title = "Apple Watch Ultra 2",
            description = "The most rugged and capable Apple Watch. Engineered for outdoor adventure and endurance sports with titanium case.",
            price = 799.99,
            rating = 4.92,
            category = "electronics",
            brand = "Apple",
            stock = 7,
            thumbnail = "https://cdn.dummyjson.com/products/images/mens-watches/Rolex%20Submariner%20Date/thumbnail.png"
        ),
        ServerProductDto(
            id = 23,
            title = "Bose SoundLink Flex Bluetooth Speaker",
            description = "Portable waterproof outdoor speaker delivering clear, balanced sound with deep bass and PositionIQ technology.",
            price = 149.99,
            rating = 4.73,
            category = "electronics",
            brand = "Bose",
            stock = 20,
            thumbnail = "https://cdn.dummyjson.com/products/images/mobile-accessories/Amazon%20Echo%20Dot/thumbnail.png"
        ),
        ServerProductDto(
            id = 24,
            title = "Ceramic Table Lamp with Linen Shade",
            description = "Handmade textured ceramic base with a warm natural linen lampshade, introducing cozy ambient lighting to any room.",
            price = 79.99,
            rating = 4.58,
            category = "home-decoration",
            brand = "Nordic Living",
            stock = 15,
            thumbnail = "https://cdn.dummyjson.com/products/images/home-decoration/House%20Plant/thumbnail.png"
        ),
        ServerProductDto(
            id = 25,
            title = "Modern Geometric Wall Art",
            description = "Set of 3 framed minimalist canvas prints featuring soothing earth-toned abstract geometric shapes.",
            price = 64.99,
            rating = 4.67,
            category = "home-decoration",
            brand = "Artisan Loft",
            stock = 18,
            thumbnail = "https://cdn.dummyjson.com/products/images/home-decoration/Key%20Holder/thumbnail.png"
        )
    )
}
