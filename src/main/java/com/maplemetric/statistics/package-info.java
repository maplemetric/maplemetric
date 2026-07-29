@ApplicationModule(
        allowedDependencies = {
                "common",
                "ranking::api",
                "world::api"
        }
)
package com.maplemetric.statistics;

import org.springframework.modulith.ApplicationModule;
