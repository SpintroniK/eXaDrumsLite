package com.example.jeremy.exadrumslite;

import java.util.List;

/**
 * Created by jeremy on 29/12/17.
 */

public class JsonQuery<T>
{
    public String jsonrpc = "2.0";
    public String method;
    public int id;
    public List<T> params;
}