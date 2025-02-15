/*
 * Copyright (C) 2018 The Sylph Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.harbby.sylph.parser.tree;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.github.harbby.gadtry.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class CreateFunction
        extends Statement
{
    private final Identifier functionName;
    private final StringLiteral classString;

    public CreateFunction(NodeLocation location, Identifier functionName, StringLiteral classString)
    {
        super(location);
        this.functionName = requireNonNull(functionName, "functionName is null");
        this.classString = requireNonNull(classString, "classString is null");
    }

    @Override
    public List<? extends Node> getChildren()
    {
        return Arrays.asList(functionName, classString);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(functionName, classString);
    }

    public String getFunctionName()
    {
        return functionName.getValue();
    }

    public String getClassString()
    {
        return classString.getValue();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        CreateFunction o = (CreateFunction) obj;
        return Objects.equals(functionName, o.functionName) &&
                Objects.equals(classString, o.classString);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("functionName", functionName)
                .add("classString", classString)
                .toString();
    }
}
