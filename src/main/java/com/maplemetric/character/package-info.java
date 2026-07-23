@ApplicationModule(
        allowedDependencies = {
                "common",
                "common::nexon",
                "ranking::api"
        }
)
package com.maplemetric.character;

import org.springframework.modulith.ApplicationModule;
