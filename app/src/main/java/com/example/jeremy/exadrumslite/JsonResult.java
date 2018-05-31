package com.example.jeremy.exadrumslite;

/**
 * Created by jeremy on 29/12/17.
 */

public class JsonResult<T>
{
    public String jsonrpc = "2.0";
    public T result;
    public int id;

}