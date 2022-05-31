import React from 'react';
import { Button } from 'reactstrap';
import { connect } from 'react-redux';
import { DynamicLayoutContext } from '../../../context';

/* eslint-disable */

function AccessTableComponent() {
    const { callAction } = React.useContext(DynamicLayoutContext);

    const clear = () => callAction({
        responseAction: {
            url: 'access/template/clear',
            targetType: 'POST',
        },
    });

    const guest = () => callAction({
        responseAction: {
            url: 'access/template/guest',
            targetType: 'POST',
        },
    });

    const employee = () => callAction({
        responseAction: {
            url: 'access/template/employee',
            targetType: 'POST',
        },
    });

    const leader = () => callAction({
        responseAction: {
            url: 'access/template/leader',
            targetType: 'POST',
        },
    });

    const administrator = () => callAction({
        responseAction: {
            url: 'access/template/administrator',
            targetType: 'POST',
        },
    });

    return React.useMemo(
        () => (
            <>
                <table>
                    <tbody>
                    <tr>
                        <th>Access management</th>
                        <td>
                            <div className="btn-group" data-toggle="buttons">

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id10"/>
                                    Select
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id11"/>
                                    Insert
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id12"/>
                                    Update
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id13"/>
                                    Delete
                                </label>

                            </div>
                        </td>
                    </tr>

                    <tr>
                        <th>Structure elements</th>
                        <td>
                            <div className="btn-group" data-toggle="buttons">

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id14"/>
                                    Select
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id15"/>
                                    Insert
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id16"/>
                                    Update
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id17"/>
                                    Delete
                                </label>

                            </div>
                        </td>
                    </tr>
                    <tr>
                        <th>Time sheets</th>
                        <td>
                            <div className="btn-group" data-toggle="buttons">

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id18"/>
                                    Select
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id19"/>
                                    Insert
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id1a"/>
                                    Update
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id1b"/>
                                    Delete
                                </label>

                            </div>
                        </td>
                    </tr>
                    <tr>
                        <th>Own time sheets</th>
                        <td>
                            <div className="btn-group" data-toggle="buttons">

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id1c"/>
                                    Select
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id1d"/>
                                    Insert
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id1e"/>
                                    Update
                                </label>

                                <label className="btn btn-xs btn-primary">
                                    <input type="checkbox" id="id1f"/>
                                    Delete
                                </label>

                            </div>
                        </td>
                    </tr>
                    </tbody>
                </table>

                <br/>


                <div>
                    Access Templates

                    <Button id={"quickselect_clear"} onClick={clear}>
                        Clear
                    </Button>

                    <Button id={"quickselect_guest"}  onClick={guest}>
                        Guest
                    </Button>

                    <Button id={"quickselect_employee"}  onClick={employee}>
                        Employee
                    </Button>

                    <Button id={"quickselect_leader"}  onClick={leader}>
                        Leader
                    </Button>

                    <Button id={"quickselect_administrator"}  onClick={administrator}>
                        Administrator
                    </Button>
                </div>
            </>
        )
    );
}

const mapStateToProps = ({ authentication }) => ({
    user: authentication.user,
});


export default connect(mapStateToProps)(AccessTableComponent);
