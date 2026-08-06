@ApplicationModule(
        allowedDependencies = {
                "common",
                "ranking::api",
                "analysis::api"
        }
)
package com.maplemetric.internal;

import org.springframework.modulith.ApplicationModule;
