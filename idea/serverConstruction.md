<Server>
    <Service>
        <Connector></Connector>
        <Connector></Connector>
        <Connector></Connector>

        <Engine>        // 用于处理servlet请求
            <Host>
                <Context></Context>
            </Host>   //Host 的意思是虚拟主机。 通常都是 localhost, 即表示本机。
            <Host>
            </Host>
        </Engine>
    <Service>
</Server>