@ApplicationModule(
        allowedDependencies = {
                "common",
                "ranking::api",
                "world::api"
        }
)
package com.maplemetric.catalog;

import org.springframework.modulith.ApplicationModule;
