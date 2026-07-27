@ApplicationModule(
        allowedDependencies = {
                "common",
                "common::nexon",
                "world::api"
        }
)
package com.maplemetric.ranking;

import org.springframework.modulith.ApplicationModule;
